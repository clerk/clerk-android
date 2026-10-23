\* Code: invalidateSession stops removing this session's tokenTasks
\* (lines 98-100), so a caller after the fence can join a task from before it.
\* Expect: NoTokenFromAnotherGeneration
Invalidate ==
  /\ invPc = "idle" /\ nInvalidations < MaxInvalidations
  /\ nInvalidations' = nInvalidations + 1
  /\ sessionGeneration' = sessionGeneration + 1
  /\ cache' = NoToken
  /\ invRelease' = NoTask
  /\ invPc' = "release"
  /\ UNCHANGED <<runtimeGeneration, task, deferreds, resetPc, resetRelease, nResets>>
  /\ CallerVarsUnchanged
