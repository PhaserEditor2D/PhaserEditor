namespace phasereditor2d.files.ui.actions {

    import controls = colibri.ui.controls;

    function isFilesViewScope(args: colibri.ui.ide.commands.CommandArgs) {
        return args.activePart instanceof views.FilesView;
    }

    export class FilesViewCommands {

        static registerCommands(manager: colibri.ui.ide.commands.CommandManager) {

            manager.addHandlerHelper(colibri.ui.ide.CMD_DELETE,

                args => isFilesViewScope(args) && DeleteFilesAction.isEnabled(args.activePart as views.FilesView),

                args => {
                    new DeleteFilesAction(args.activePart as views.FilesView).run();
                });

            manager.addHandlerHelper(colibri.ui.ide.CMD_RENAME,

                args => isFilesViewScope(args) && RenameFileAction.isEnabled(args.activePart as views.FilesView),

                args => {
                    new RenameFileAction(args.activePart as views.FilesView).run();
                });
        }
    }
}