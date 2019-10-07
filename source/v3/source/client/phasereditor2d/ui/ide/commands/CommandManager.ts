namespace phasereditor2d.ui.ide.commands {

    export class CommandManager {

        private _commandMap: Map<String, Command>;
        private _commands: Command[];
        private _handlerMatcherMap: Map<string, KeyMatcher[]>;
        private _handlerCommandMap: Map<Command, CommandHandler[]>;
        private _handlerMap: Map<string, CommandHandler>;

        constructor() {
            this._commands = [];
            this._commandMap = new Map();
            this._handlerMap = new Map();
            this._handlerMatcherMap = new Map();
            this._handlerCommandMap = new Map();

            window.addEventListener("keydown", e => { this.onKeyDown(e); })
        }

        private onKeyDown(event: KeyboardEvent): void {
            if (event.isComposing) {
                return;
            }

            const args = this.makeArgs();

            for (const command of this._commands) {

                const handlers = this._handlerCommandMap.get(command);

                for (const handler of handlers) {

                    let eventMatches = false;

                    const matchers = this.getMatchers(handler.getId());

                    for (const matcher of matchers) {

                        if (matcher.matches(event)) {

                            event.preventDefault();

                            eventMatches = true;

                            break;
                        }
                    }

                    if (eventMatches) {

                        if (handler.test(args)) {
                            handler.execute(args);
                            return;
                        }
                    }
                }
            }
        }

        addCommand(cmd: Command): void {
            this._commandMap.set(cmd.getId(), cmd);
            this._commands.push(cmd);
            this._handlerCommandMap.set(cmd, []);
        }

        getCommand(id: string): Command {
            return this._commandMap.get(id);
        }

        private makeArgs() {

            const wb = Workbench.getWorkbench();

            return new CommandArgs(
                wb.getActivePart(),
                wb.getActiveEditor()
            );
        }

        addKeyBinding(handlerId: string, matcher: KeyMatcher): void {

            const handler = this._handlerMap.get(handlerId);

            if (handler) {
                this.getMatchers(handlerId).push(matcher);
            } else {
                console.warn(`Handler ${handler.getId()} not found.`);
            }

        }

        addHandler(commandId: string, handler: CommandHandler) {

            this._handlerMap.set(handler.getId(), handler);

            const command = this._commandMap.get(commandId);

            if (command) {
                this._handlerCommandMap.get(command).push(handler);
            } else {
                console.warn(`Command ${handler.getId()} not found.`);
            }
        }

        private getMatchers(handlerId: string) {
            let matchers = this._handlerMatcherMap.get(handlerId);

            if (matchers === undefined) {
                matchers = [];
                this._handlerMatcherMap.set(handlerId, matchers);
            }

            return matchers;
        }

    }

}