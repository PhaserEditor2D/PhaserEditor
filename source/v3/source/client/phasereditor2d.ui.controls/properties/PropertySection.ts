namespace phasereditor2d.ui.controls.properties {

    export declare type Updater = () => {};

    export abstract class PropertySection<T> {

        private _id: string;
        private _title: string;
        private _page: PropertyPage;
        private _updaters: Updater[];

        constructor(page: PropertyPage, id: string, title: string) {
            this._page = page;
            this._id = id;
            this._title = title;
            this._updaters = [];
        }

        protected abstract createForm(parent: HTMLDivElement);

        abstract canEdit(obj: any): boolean;

        abstract canEditNumber(n: number): boolean;

        updateWithSelection(): void {
            for (const updater of this._updaters) {
                updater();
            }
        }

        addUpdater(updater: Updater) {
            this._updaters.push(updater);
        }

        getPage() {
            return this._page;
        }

        *getSelection() {
            for (const obj of this._page.getSelection()) {
                yield <T>obj;
            }
        }

        getId() {
            return this._id;
        }

        getTitle() {
            return this._title;
        }

        create(parent: HTMLDivElement): void {
            this.createForm(parent);
        }
    }
}