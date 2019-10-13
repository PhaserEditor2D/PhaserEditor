namespace phasereditor2d.files.ui.viewers {

    import controls = colibri.ui.controls;

    export abstract class ContentTypeCellRendererExtension extends colibri.core.extensions.Extension {

        static POINT = "phasereditor2d.files.ui.viewers.ContentTypeCellRendererExtension";

        constructor(id : string) {
            super(id);
        }

        abstract getRendererProvider(contentType : string): controls.viewers.ICellRendererProvider;

    } 
}