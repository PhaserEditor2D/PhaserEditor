/// <reference path="./commands/KeyMatcher.ts" />

namespace colibri.ui.ide {

    import KeyMatcher = commands.KeyMatcher;

    export const CMD_SAVE = "save";
    export const CMD_DELETE = "delete";
    export const CMD_RENAME = "rename";
    export const CMD_UNDO = "undo";
    export const CMD_REDO = "redo";
    export const CMD_SWITCH_THEME = "switchTheme";
    export const CMD_COLLAPSE_ALL = "collapseAll";
    export const CMD_EXPAND_COLLAPSE_BRANCH = "expandCollapseBranch";

    export class IDECommands {

        static init() {
            const manager = Workbench.getWorkbench().getCommandManager();

            this.initEdit(manager);

            this.initUndo(manager);

            this.initTheme(manager);

            this.initViewer(manager);
        }

        private static initViewer(manager: commands.CommandManager) {

            // collapse all

            manager.addCommandHelper(CMD_COLLAPSE_ALL);

            manager.addHandlerHelper(CMD_COLLAPSE_ALL,
                args => args.activeElement !== null && controls.Control.getControlOf(args.activeElement) instanceof controls.viewers.Viewer,
                args => {
                    const viewer = <controls.viewers.Viewer>controls.Control.getControlOf(args.activeElement);
                    viewer.collapseAll();
                    viewer.repaint();
                }
            );

            manager.addKeyBinding(CMD_COLLAPSE_ALL, new KeyMatcher({
                key: "c"
            }))

            // collapse expand branch

            manager.addCommandHelper(CMD_EXPAND_COLLAPSE_BRANCH);

            manager.addHandlerHelper(CMD_EXPAND_COLLAPSE_BRANCH,
                args => args.activeElement !== null && controls.Control.getControlOf(args.activeElement) instanceof controls.viewers.Viewer,
                args => {
                    const viewer = <controls.viewers.Viewer>controls.Control.getControlOf(args.activeElement);

                    const parents = [];

                    for (const obj of viewer.getSelection()) {
                        const objParents = viewer.expandCollapseBranch(obj);
                        parents.push(...objParents);
                    }

                    viewer.setSelection(parents);
                }
            );

            manager.addKeyBinding(CMD_EXPAND_COLLAPSE_BRANCH, new KeyMatcher({
                key: " "
            }))
        }

        private static initTheme(manager: commands.CommandManager) {

            manager.addCommandHelper(CMD_SWITCH_THEME);

            manager.addHandlerHelper(CMD_SWITCH_THEME,
                args => true,
                args => controls.Controls.switchTheme()
            );

            manager.addKeyBinding(CMD_SWITCH_THEME, new KeyMatcher({
                control: true,
                key: "2"
            }));
        }

        private static initUndo(manager: commands.CommandManager) {
            // undo

            manager.addCommandHelper(CMD_UNDO);

            manager.addHandlerHelper(CMD_UNDO,
                args => args.activePart !== null,
                args => args.activePart.getUndoManager().undo()
            );

            manager.addKeyBinding(CMD_UNDO, new KeyMatcher({
                control: true,
                key: "z"
            }));


            // redo

            manager.addCommandHelper(CMD_REDO);

            manager.addHandlerHelper(CMD_REDO,
                args => args.activePart !== null,
                args => args.activePart.getUndoManager().redo()

            );

            manager.addKeyBinding(CMD_REDO, new KeyMatcher({
                control: true,
                shift: true,
                key: "z"
            }));

        }

        private static initEdit(manager: commands.CommandManager) {

            // save

            manager.addCommandHelper(CMD_SAVE);

            manager.addHandlerHelper(CMD_SAVE,

                args => args.activeEditor && args.activeEditor.isDirty(),

                args => {
                    args.activeEditor.save();
                }
            );

            manager.addKeyBinding(CMD_SAVE, new KeyMatcher({
                control: true,
                key: "s",
                filterInputElements: false
            }));

            // delete

            manager.addCommandHelper(CMD_DELETE);

            manager.addKeyBinding(CMD_DELETE, new KeyMatcher({
                key: "delete"
            }));

            // rename

            manager.addCommandHelper(CMD_RENAME);

            manager.addKeyBinding(CMD_RENAME, new KeyMatcher({
                key: "f2"
            }));
        }

    }

}