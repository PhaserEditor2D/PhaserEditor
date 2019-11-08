namespace colibri.ui.ide {

    export class FileUtils {

        static getImage(file: core.io.FilePath) {
            return Workbench.getWorkbench().getFileImage(file);
        }

        static async preloadAndGetFileString(file: core.io.FilePath): Promise<string> {
            
            await this.preloadFileString(file);

            return this.getFileString(file);
        }

        static getFileString(file: core.io.FilePath): string {
            return Workbench.getWorkbench().getFileStringCache().getContent(file);
        }

        static setFileString_async(file: core.io.FilePath, content: string): Promise<void> {
            return Workbench.getWorkbench().getFileStringCache().setContent(file, content);
        }

        static async preloadFileString(file: core.io.FilePath): Promise<ui.controls.PreloadResult> {

            const cache = Workbench.getWorkbench().getFileStringCache();

            return cache.preload(file);
        }

        static getFileFromPath(path: string): core.io.FilePath {
            const root = Workbench.getWorkbench().getProjectRoot();

            const names = path.split("/");

            let result = root;

            for (const name of names) {
                const child = result.getFile(name);
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

            Workbench.getWorkbench().getProjectRoot().flatTree(files, false);

            return files;
        }

        static getRoot() {
            return Workbench.getWorkbench().getProjectRoot();
        }

    }
}