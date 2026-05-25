# AI Migration Path

## Tooling

- [tmux](https://tmux.info/) Terminal Multiplexer
- [Cucmumber](https://cucumber.io/) Gherkin BDD Testing
- [crap-java-maven-plugin](https://github.com/fabian-barney/crap-java) Change Risk Analysis and Predictions
- [pitest-maven](https://pitest.org/) Real world mutation testing

This is the minimum required to run the swarm. More can be added later.

## Terminal Setup

```text
tmux terminal context windows

┌─────────────────┬─────────────────┐
│ Architect       │ Coder           │
│                 │                 │
│                 │                 │
├─────────────────┼─────────────────┤
│ E2E-Interpreter │ Monitor         │
│                 │                 │
│                 │                 │
└─────────────────┴─────────────────┘

Usage: $>./swarmforge start
```

```text
context window communication

┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ Architect       │   │ E2E-Interpreter │   │ Coder           │   │ Monitor         │
│                 │   │                 │   │                 │   │                 │
│                 │   │                 │   │                 │   │                 │
└─────────────────┘   └─────────────────┘   └─────────────────┘   └─────────────────┘
         ↑                           ↑          ↑                     ↑ 
         └─────────────────────┐     │          │     ┌───────────────┘ 
                               ↓     ↓          ↓     │
                             ┌──────────────────────────┐
                             │ logs/agent_meessages.log │
                             │ agent_context/*.json     │ 
                             │ .swarmforge/defects.md   │
                             │ ...                      │
                             └──────────────────────────┘ 
                             
each context windows is listening to changes in theses files via watch command 
```


## AI

``` text
.swarmforge
├── Constitution.md
├── defects.md
└── prompts
    ├── Architect.md
    ├── Coder.md
    └── E2E-Interpreter.md
logs
└── agent_messages.log
```

### Architect

Uses prompt `.swarmforge/prompts/Architect.md`. The Constitution is the source of truth for all agents and defines the rules they must follow (e.g., TDD, Gherkin, mutation testing, complexity limits, linter rules, etc.). The Architect is responsible for updating the Constitution as needed and ensuring all agents read it at startup.

### E2E Interpreter

Uses prompt `.swarmforge/prompts/E2E-Interpreter.md`. The E2E Interpreter is responsible for interpreting the user's input and generating the appropriate commands for the Coder.

### Coder

Uses prompt `.swarmforge/prompts/Coder.md`. The Coder is responsible for implementing the user's requirements and ensuring that they are met. The Coder must follow the rules defined in the Constitution and work closely with the E2E Interpreter to ensure that the implementation meets the user's requirements.

### Monitor

Shows the content from the agent_mesages.log file in real time, as well as any other relevant metrics or dashboards (e.g., mutation kill rate, code complexity trends, etc.) that can be derived from the swarm's activity. This allows the human maintainer to observe the swarm's behavior and intervene if necessary.
