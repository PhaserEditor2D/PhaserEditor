namespace phasereditor2d.pack.ui.viewers {

    import controls = colibri.ui.controls;

    export class AssetPackLabelProvider implements controls.viewers.ILabelProvider {

        getLabel(obj: any): string {
            if (obj instanceof core.AssetPack) {
                return obj.getFile().getName();
            }

            if (obj instanceof core.AssetPackItem) {
                return obj.getKey();
            }

            if (obj instanceof controls.ImageFrame) {
                return obj.getName();
            }

            if (typeof (obj) === "string") {
                return obj;
            }

            return "";
        }

    }

}