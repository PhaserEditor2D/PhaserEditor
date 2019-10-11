namespace phasereditor2d.ui.ide {

    export abstract class Plugin {

        private _id: string;

        constructor(id: string) {
            this._id = id;
        }

        getId() {
            return this._id;
        }

        preloadIcons(): Promise<void> {
            return Promise.resolve();
        }

        registerContentTypes(registry: core.ContentTypeRegistry): void {
            
        }

        preloadProjectResources(): Promise<void> {
            return Promise.resolve();
        }

        registerCommands(manager: commands.CommandManager): void {

        }

        registerEditor(registry: EditorRegistry): void {

        }

        createWindow(windows: ide.Window[]): void {

        }

    }

}