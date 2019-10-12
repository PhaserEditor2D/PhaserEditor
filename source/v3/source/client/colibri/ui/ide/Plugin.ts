namespace colibri.ui.ide {

    export abstract class Plugin {

        private _id: string;

        constructor(id: string) {
            this._id = id;
        }

        getId() {
            return this._id;
        }

        starting() : Promise<void> {
            return Promise.resolve();
        }

        started() : Promise<void> {
            return Promise.resolve();
        }

        preloadIcons(contentTypeIconMap : Map<string, controls.IImage>): Promise<void> {
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

        createWindow(windows: ide.WorkbenchWindow[]): void {

        }

    }

}