namespace colibri.ui.controls.viewers {

    export class IconImageCellRenderer extends ImageCellRenderer {

        private _icon: IImage;

        constructor(icon: IImage) {
            super();

            this._icon = icon;
        }

        getImage() {
            return this._icon;
        }

        cellHeight(args: RenderCellArgs) {
            return ROW_HEIGHT;
        }
    }

}