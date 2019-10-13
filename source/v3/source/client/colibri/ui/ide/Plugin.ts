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

        registerExtensions(registry : core.extensions.ExtensionRegistry) : void {

        }

        registerContentTypeIcons(contentTypeIconMap : Map<string, controls.IImage>): void {
            
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

        getIcon(name : string) {
            return controls.Controls.getIcon(name, `plugins/${this.getId()}/ui/icons`);
        }

    }

}