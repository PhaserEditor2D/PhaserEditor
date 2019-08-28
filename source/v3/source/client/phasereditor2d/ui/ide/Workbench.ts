/// <reference path="../../../phasereditor2d.ui.controls/Controls.ts"/>
/// <reference path="../../../phasereditor2d.ui.controls/PaddingPanel.ts"/>
/// <reference path="../ide/ViewPart.ts"/>
/// <reference path="../ide/DesignWindow.ts"/>

namespace phasereditor2d.ui {

    export class Workbench {
        private static _workbench: Workbench;


        static getWorkbench() {
            if (!Workbench._workbench) {
                Workbench._workbench = new Workbench();
            }

            return this._workbench;
        }

        private _designWindow: ide.DesignWindow;
        private _contentType_icon_Map: Map<string, controls.IIcon>;

        private constructor() {
            this._contentType_icon_Map = new Map();
            this._contentType_icon_Map.set("img", controls.Controls.getIcon(controls.Controls.ICON_FILE_IMAGE));
            this._contentType_icon_Map.set("sound", controls.Controls.getIcon(controls.Controls.ICON_FILE_SOUND));
            this._contentType_icon_Map.set("video", controls.Controls.getIcon(controls.Controls.ICON_FILE_VIDEO));
            this._contentType_icon_Map.set("js", controls.Controls.getIcon(controls.Controls.ICON_FILE_SCRIPT));
            this._contentType_icon_Map.set("ts", controls.Controls.getIcon(controls.Controls.ICON_FILE_SCRIPT));
            this._contentType_icon_Map.set("json", controls.Controls.getIcon(controls.Controls.ICON_FILE_SCRIPT));
            this._contentType_icon_Map.set("txt", controls.Controls.getIcon(controls.Controls.ICON_FILE_TEXT));
        }

        start() {
            this._designWindow = new ide.DesignWindow();
            document.getElementById("body").appendChild(this._designWindow.getElement());
        }

        getContentTypeIcon(contentType: string): controls.IIcon {
            if (this._contentType_icon_Map.has(contentType)) {
                return this._contentType_icon_Map.get(contentType);
            }
            return null;
        }

    }

    
}