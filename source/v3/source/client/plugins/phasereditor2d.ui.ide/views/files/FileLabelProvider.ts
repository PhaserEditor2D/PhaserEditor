
namespace phasereditor2d.ui.ide.views.files {

    import io = phasereditor2d.core.io;
    import viewers = phasereditor2d.ui.controls.viewers;

    export class FileLabelProvider implements viewers.ILabelProvider {
        getLabel(obj: io.FilePath): string {
            return obj.getName();
        }
    }
}