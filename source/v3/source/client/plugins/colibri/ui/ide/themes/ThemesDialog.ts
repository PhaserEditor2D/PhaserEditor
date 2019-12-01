namespace colibri.ui.ide.themes {

    export class ThemesDialog extends ui.controls.dialogs.ViewerDialog {

        constructor() {
            super(new ThemeViewer());

            this.setSize(200, 300);
        }

        create() {

            super.create();

            this.setTitle("Themes");

            this.addButton("Close", () => this.close());
        }
    }


    class ThemeViewer extends controls.viewers.TreeViewer {

        constructor() {
            super("ThemeViewer");

            this.setLabelProvider(new ThemeLabelProvider());
            this.setContentProvider(new controls.viewers.ArrayTreeContentProvider());
            this.setCellRendererProvider(new controls.viewers.EmptyCellRendererProvider());
            this.setInput(
                Workbench.getWorkbench()
                    .getExtensionRegistry()
                    .getExtensions<ThemeExtension>(ThemeExtension.POINT_ID)
                    .map(ext => ext.getTheme())
            );
        }
    }

    class ThemeLabelProvider extends controls.viewers.LabelProvider {

        getLabel(theme: controls.Theme) {
            return theme.displayName;
        }
    }
}