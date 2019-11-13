declare namespace phasereditor2d.files {
    import ide = colibri.ui.ide;
    const ICON_NEW_FILE = "file-new";
    const ICON_FILE_FONT = "file-font";
    const ICON_FILE_IMAGE = "file-image";
    const ICON_FILE_VIDEO = "file-movie";
    const ICON_FILE_SCRIPT = "file-script";
    const ICON_FILE_SOUND = "file-sound";
    const ICON_FILE_TEXT = "file-text";
    class FilesPlugin extends ide.Plugin {
        private static _instance;
        static getInstance(): FilesPlugin;
        private constructor();
        registerExtensions(reg: colibri.core.extensions.ExtensionRegistry): void;
    }
}
declare namespace phasereditor2d.files.core {
    import io = colibri.core.io;
    class ExtensionContentTypeResolver extends colibri.core.ContentTypeResolver {
        private _map;
        constructor(id: string, defs: string[][]);
        computeContentType(file: io.FilePath): Promise<string>;
    }
}
declare namespace phasereditor2d.files.core {
    const CONTENT_TYPE_IMAGE = "image";
    const CONTENT_TYPE_SVG = "svg";
    const CONTENT_TYPE_AUDIO = "audio";
    const CONTENT_TYPE_VIDEO = "video";
    const CONTENT_TYPE_SCRIPT = "script";
    const CONTENT_TYPE_TEXT = "text";
    const CONTENT_TYPE_CSV = "csv";
    const CONTENT_TYPE_JAVASCRIPT = "javascript";
    const CONTENT_TYPE_HTML = "html";
    const CONTENT_TYPE_CSS = "css";
    const CONTENT_TYPE_JSON = "json";
    const CONTENT_TYPE_XML = "xml";
    const CONTENT_TYPE_GLSL = "glsl";
    class DefaultExtensionTypeResolver extends ExtensionContentTypeResolver {
        constructor();
    }
}
declare namespace phasereditor2d.files.ui.actions {
    import controls = colibri.ui.controls;
    import io = colibri.core.io;
    class OpenNewFileDialogAction extends controls.Action {
        private _initialLocation;
        constructor();
        run(): void;
        private openFileDialog;
        setInitialLocation(folder: io.FilePath): void;
    }
}
declare namespace phasereditor2d.files.ui.dialogs {
    import controls = colibri.ui.controls;
    import io = colibri.core.io;
    type CreateFileCallback = (folder: io.FilePath, filename: string) => void;
    abstract class BaseNewFileDialog extends controls.dialogs.Dialog {
        protected _filteredViewer: controls.viewers.FilteredViewerInElement<controls.viewers.TreeViewer>;
        protected _fileNameText: HTMLInputElement;
        private _createBtn;
        private _fileCreatedCallback;
        constructor();
        protected createDialogArea(): void;
        private createBottomArea;
        protected normalizedFileName(): string;
        validate(): void;
        setFileCreatedCallback(callback: (file: io.FilePath) => void): void;
        setInitialFileName(filename: string): void;
        setInitialLocation(folder: io.FilePath): void;
        create(): void;
        private createFile_priv;
        protected abstract createFile(container: io.FilePath, name: string): Promise<io.FilePath>;
        private createCenterArea;
        private createFilteredViewer;
        layout(): void;
    }
}
declare namespace phasereditor2d.files.ui.dialogs {
    import controls = colibri.ui.controls;
    import io = colibri.core.io;
    abstract class NewFileExtension extends colibri.core.extensions.Extension {
        static POINT: string;
        private _wizardName;
        private _icon;
        private _initialFileName;
        constructor(config: {
            id: string;
            wizardName: string;
            icon: controls.IImage;
            initialFileName: string;
        });
        abstract createDialog(): BaseNewFileDialog;
        getInitialFileName(): string;
        getWizardName(): string;
        getIcon(): controls.IImage;
        getInitialFileLocation(): io.FilePath;
        findInitialFileLocationBasedOnContentType(contentType: string): io.FilePath;
    }
}
declare namespace phasereditor2d.files.ui.dialogs {
    import controls = colibri.ui.controls;
    abstract class NewFileContentExtension extends NewFileExtension {
        private _fileExtension;
        private _fileContent;
        constructor(config: {
            id: string;
            wizardName: string;
            icon: controls.IImage;
            initialFileName: string;
            fileExtension: string;
            fileContent: string;
        });
        createDialog(): NewFileDialog;
    }
}
declare namespace phasereditor2d.files.ui.dialogs {
    import io = colibri.core.io;
    class NewFileDialog extends BaseNewFileDialog {
        private _fileExtension;
        private _fileContent;
        constructor();
        protected normalizedFileName(): string;
        setFileContent(fileContent: string): void;
        setFileExtension(fileExtension: string): void;
        protected createFile(folder: io.FilePath, name: string): Promise<io.FilePath>;
    }
}
declare namespace phasereditor2d.files.ui.dialogs {
    class NewFolderDialog extends BaseNewFileDialog {
        protected createFile(container: colibri.core.io.FilePath, name: string): Promise<colibri.core.io.FilePath>;
    }
}
declare namespace phasereditor2d.files.ui.dialogs {
    class NewFolderExtension extends NewFileExtension {
        constructor();
        createDialog(): BaseNewFileDialog;
    }
}
declare namespace phasereditor2d.files.ui.viewers {
    import controls = colibri.ui.controls;
    abstract class ContentTypeCellRendererExtension extends colibri.core.extensions.Extension {
        static POINT: string;
        constructor(id: string);
        abstract getRendererProvider(contentType: string): controls.viewers.ICellRendererProvider;
    }
}
declare namespace phasereditor2d.files.ui.viewers {
    import viewers = colibri.ui.controls.viewers;
    import controls = colibri.ui.controls;
    class FileCellRenderer extends viewers.IconImageCellRenderer {
        constructor();
        getIcon(obj: any): controls.IImage;
        preload(obj: any): Promise<controls.PreloadResult>;
    }
}
declare namespace phasereditor2d.files.ui.viewers {
    import controls = colibri.ui.controls;
    import io = colibri.core.io;
    import viewers = colibri.ui.controls.viewers;
    class FileCellRendererProvider implements viewers.ICellRendererProvider {
        private _layout;
        constructor(layout?: "tree" | "grid");
        getCellRenderer(file: io.FilePath): viewers.ICellRenderer;
        preload(file: io.FilePath): Promise<controls.PreloadResult>;
    }
}
declare namespace phasereditor2d.files.ui.viewers {
    import viewers = colibri.ui.controls.viewers;
    import io = colibri.core.io;
    class FileLabelProvider implements viewers.ILabelProvider {
        getLabel(obj: io.FilePath): string;
    }
}
declare namespace phasereditor2d.files.ui.viewers {
    import controls = colibri.ui.controls;
    class FileTreeContentProvider implements controls.viewers.ITreeContentProvider {
        private _onlyFolders;
        constructor(onlyFolders?: boolean);
        getRoots(input: any): any[];
        getChildren(parent: any): any[];
    }
}
declare namespace phasereditor2d.files.ui.viewers {
    import controls = colibri.ui.controls;
    class SimpleContentTypeCellRendererExtension extends colibri.core.extensions.Extension {
        private _contentType;
        private _cellRenderer;
        constructor(contentType: string, cellRenderer: controls.viewers.ICellRenderer);
        getRendererProvider(contentType: string): controls.viewers.ICellRendererProvider;
    }
}
declare namespace phasereditor2d.files.ui.views {
    import controls = colibri.ui.controls;
    class FilePropertySectionProvider extends controls.properties.PropertySectionProvider {
        addSections(page: controls.properties.PropertyPage, sections: controls.properties.PropertySection<any>[]): void;
    }
}
declare namespace phasereditor2d.files.ui.views {
    import controls = colibri.ui.controls;
    import core = colibri.core;
    class FileSection extends controls.properties.PropertySection<core.io.FilePath> {
        constructor(page: controls.properties.PropertyPage);
        protected createForm(parent: HTMLDivElement): void;
        canEdit(obj: any): boolean;
        canEditNumber(n: number): boolean;
    }
}
declare namespace phasereditor2d.files.ui.views {
    import controls = colibri.ui.controls;
    import ide = colibri.ui.ide;
    class FilesView extends ide.ViewerView {
        private _propertyProvider;
        constructor();
        protected createViewer(): controls.viewers.TreeViewer;
        fillContextMenu(menu: controls.Menu): void;
        private onNewFile;
        private onRenameFile;
        getPropertyProvider(): FilePropertySectionProvider;
        protected createPart(): void;
        getIcon(): controls.IImage;
    }
}
declare namespace phasereditor2d.files.ui.views {
    import controls = colibri.ui.controls;
    import core = colibri.core;
    class ImageFileSection extends controls.properties.PropertySection<core.io.FilePath> {
        constructor(page: controls.properties.PropertyPage);
        protected createForm(parent: HTMLDivElement): void;
        canEdit(obj: any): boolean;
        canEditNumber(n: number): boolean;
    }
}
declare namespace phasereditor2d.files.ui.views {
    import controls = colibri.ui.controls;
    import core = colibri.core;
    class ManyImageFileSection extends controls.properties.PropertySection<core.io.FilePath> {
        constructor(page: controls.properties.PropertyPage);
        protected createForm(parent: HTMLDivElement): void;
        canEdit(obj: any): boolean;
        canEditNumber(n: number): boolean;
    }
}
//# sourceMappingURL=plugin.d.ts.map