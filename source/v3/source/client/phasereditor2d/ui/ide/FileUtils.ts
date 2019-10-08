namespace phasereditor2d.ui.ide {

    export class FileUtils {

        static getImage(file: core.io.FilePath) {
            return Workbench.getWorkbench().getFileImage(file);
        }

        static getFileStringFromCache(file : core.io.FilePath) {
            return Workbench.getWorkbench().getFileStorage().getFileStringFromCache(file);
        }

        static getFileString(file : core.io.FilePath) {
            return Workbench.getWorkbench().getFileStorage().getFileString(file);
        }

        static async preloadFileString(file: core.io.FilePath): Promise<ui.controls.PreloadResult> {
            const storage = Workbench.getWorkbench().getFileStorage();

            if (storage.hasFileStringInCache(file)) {
                return controls.Controls.resolveNothingLoaded();
            }

            await storage.getFileString(file);

            return controls.Controls.resolveResourceLoaded();
        }

        static getFileFromPath(path: string): core.io.FilePath {
            const root = Workbench.getWorkbench().getFileStorage().getRoot();

            const names = path.split("/");

            let result = root;

            for (const name of names) {
                const child = result.getFiles().find(file => file.getName() === name);
                if (child) {
                    result = child;
                } else {
                    return null;
                }
            }

            return result;
        }

        static async getFilesWithContentType(contentType: string) {
            const reg = Workbench.getWorkbench().getContentTypeRegistry();

            const files = this.getAllFiles();

            for (const file of files) {
                await reg.preload(file);
            }

            return files.filter(file => reg.getCachedContentType(file) === contentType);
        }

        static getAllFiles() {
            const files: core.io.FilePath[] = [];
            this.getAllFiles2(Workbench.getWorkbench().getFileStorage().getRoot(), files);
            return files;
        }

        private static getAllFiles2(folder: core.io.FilePath, files: core.io.FilePath[]) {
            for (const file of folder.getFiles()) {
                if (file.isFolder()) {
                    this.getAllFiles2(file, files);
                } else {
                    files.push(file);
                }
            }
        }
    }
}