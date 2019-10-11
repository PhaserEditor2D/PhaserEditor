namespace phasereditor2d.core.io {

    export declare type GetFileContent<T> = (file: FilePath) => Promise<T>;

    export declare type SetFileContent<T> = (file: FilePath, content: T) => Promise<void>;

    export class FileContentCache<T> {

        private _getContent: GetFileContent<T>;
        private _setContent: SetFileContent<T>;
        private _map: Map<string, ContentEntry<T>>;

        constructor(getContent: GetFileContent<T>, setContent?: SetFileContent<T>) {

            this._getContent = getContent;
            this._setContent = setContent;

            this._map = new Map();
        }

        preload(file: FilePath): Promise<ui.controls.PreloadResult> {

            const entry = this._map.get(file.getFullName());

            if (entry) {

                if (entry.modTime === file.getModTime()) {
                    return ui.controls.Controls.resolveNothingLoaded();
                }

                return this._getContent(file)

                    .then((content) => {

                        entry.modTime = file.getModTime();
                        entry.content = content;

                        return ui.controls.PreloadResult.RESOURCES_LOADED;
                    });
            }

            return this._getContent(file)

                .then((content) => {

                    this._map.set(file.getFullName(), new ContentEntry(content, file.getModTime()));

                    return ui.controls.PreloadResult.RESOURCES_LOADED;
                });

        }

        getContent(file: FilePath) {

            const entry = this._map.get(file.getFullName());

            return entry ? entry.content : null;

        }

        async setContent(file: FilePath, content: T): Promise<void> {

            if (this._setContent) {
                await this._setContent(file, content);
            }

            const name = file.getFullName();
            const modTime = file.getModTime();

            let entry = this._map.get(name);

            if (entry) {

                entry.content = content;
                entry.modTime = modTime;

            } else {
                this._map.set(name, entry = new ContentEntry(content, modTime));
            }
        }

        hasFile(file: FilePath) {
            return this._map.has(file.getFullName());
        }
    }

    class ContentEntry<T> {
        constructor(
            public content: T,
            public modTime: number
        ) {
        }
    }

}