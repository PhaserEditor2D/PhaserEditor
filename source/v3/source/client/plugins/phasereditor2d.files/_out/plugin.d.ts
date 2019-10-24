declare namespace phasereditor2d.files {
    import ide = colibri.ui.ide;
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
    class DefaultExtensionTypeResolver extends ExtensionContentTypeResolver {
        constructor();
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
    class FileCellRenderer extends viewers.LabelCellRenderer {
        getImage(obj: any): controls.IImage;
        preload(obj: any): Promise<any>;
    }
}
declare namespace phasereditor2d.files.ui.viewers {
    import controls = colibri.ui.controls;
    import io = colibri.core.io;
    import viewers = colibri.ui.controls.viewers;
    class FileCellRendererProvider implements viewers.ICellRendererProvider {
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