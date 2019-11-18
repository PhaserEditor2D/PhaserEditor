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

        getIcon(name : string) : controls.IImage {
            return controls.Controls.getIcon(name, `plugins/${this.getId()}/ui/icons`);
        }

    }

}