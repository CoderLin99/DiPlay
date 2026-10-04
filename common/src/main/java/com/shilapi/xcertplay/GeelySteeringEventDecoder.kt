package com.shilapi.xcertplay

/** Normalizes OneOS aliases before filtering; reports observations even when not forwarded. */
internal class GeelySteeringEventDecoder(
    private val record: (String) -> Unit,
    private val emit: (GeelySteeringKeyEvent) -> Unit,
) {
    private val down = mutableSetOf<Int>()
    private val lastUp = mutableMapOf<Int, Long>()

    @Synchronized
    fun accept(transaction: Int, raw: Int, action: Int, now: Long) {
        val key = GeelySteeringKeyCodes.canonicalize(raw)
        var result = "ignored"
        val event = if (key == null) null else when (transaction) {
            1 -> when (action) {
                GeelySteeringKeyEvent.ACTION_DOWN ->
                    if (down.add(key)) GeelySteeringKeyEvent(key, raw, action, now) else null
                GeelySteeringKeyEvent.ACTION_UP -> {
                    val wasDown = down.remove(key)
                    lastUp[key] = now
                    if (wasDown) GeelySteeringKeyEvent(key, raw, action, now) else null
                }
                else -> null
            }
            2, 5, 6 -> {
                val followsRaw = key in down || lastUp[key]?.let { now - it in 0 until 1_500L } == true
                val gesture = when (transaction) {
                    2 -> GeelySteeringKeyEvent.ACTION_SINGLE
                    5 -> GeelySteeringKeyEvent.ACTION_LONG
                    else -> GeelySteeringKeyEvent.ACTION_DOUBLE
                }
                if (followsRaw) null else GeelySteeringKeyEvent(key, raw, gesture, now)
            }
            else -> null
        }
        if (event != null) result = "forwarded"
        record("oneOs raw=$raw canonical=${key ?: -1} transaction=$transaction action=$action result=$result")
        event?.let(emit)
    }
}
