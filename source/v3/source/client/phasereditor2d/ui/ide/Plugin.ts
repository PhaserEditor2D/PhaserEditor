namespace phasereditor2d.ui.ide {

    export abstract class Plugin {
        private _id: string;

        constructor(id: string) {
            this._id = id;
        }

        getId() {
            return this._id;
        }

        abstract preloadIcons(workbench: Workbench): Promise<void>;

        abstract registerContentTypes(workbench: Workbench, registry: core.ContentTypeRegistry): void;

        abstract preloadProjectResources(): Promise<void>;

        abstract registerCommands(manager: commands.CommandManager): void;

        abstract registerEditor(registry: EditorRegistry): void;

        abstract createWindow(windows: ide.Window[]): void;

    }

}