package android.kma.myquizzapp.core.common.model

/** Read-only identity of a session lifetime; profile field changes do not change it. */
data class SessionIdentity(val userId: Long?, val generation: Long, val resolved: Boolean)

/** State and generation must be captured together by the production repository. */
data class SessionSnapshot(val state: SessionState, val generation: Long) {
    val identity: SessionIdentity
        get() = SessionIdentity(state.userOrNull?.id, generation, state !is SessionState.Unknown)
}
