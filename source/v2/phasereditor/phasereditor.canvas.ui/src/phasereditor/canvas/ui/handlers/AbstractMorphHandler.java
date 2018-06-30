package phasereditor.canvas.ui.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;

import phasereditor.assetpack.core.BitmapFontAssetModel;
import phasereditor.assetpack.core.IAssetKey;
import phasereditor.assetpack.ui.SelectTextureDialog;
import phasereditor.canvas.core.AssetSpriteModel;
import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.BitmapTextModel;
import phasereditor.canvas.core.ITextSpriteModel;
import phasereditor.canvas.core.TextModel;
import phasereditor.canvas.ui.editors.CanvasEditor;
import phasereditor.canvas.ui.editors.ObjectCanvas;
import phasereditor.canvas.ui.editors.grid.editors.BitmapTextFontDialog;
import phasereditor.canvas.ui.editors.operations.AddNodeOperation;
import phasereditor.canvas.ui.editors.operations.CompositeOperation;
import phasereditor.canvas.ui.editors.operations.DeleteNodeOperation;
import phasereditor.canvas.ui.editors.operations.SelectOperation;
import phasereditor.canvas.ui.shapes.GroupNode;
import phasereditor.canvas.ui.shapes.IObjectNode;
import phasereditor.canvas.ui.shapes.ISpriteNode;
import phasereditor.ui.properties.TextDialog;

/**
 * 
 * @author arian
 *
 */
public abstract class AbstractMorphHandler<T extends BaseObjectModel> extends AbstractHandler {

	private Class<T> _morphToType;

	public AbstractMorphHandler(Class<T> morphToType) {
		_morphToType = morphToType;
	}

	protected static class MorphToArgs {
		// nothing
	}

	protected static class MorphToTextArgs extends MorphToArgs {
		public String text;
		public int size;

		public MorphToTextArgs(String text, int size) {
			super();
			this.text = text;
			this.size = size;
		}

	}

	protected static class MorphToBitmapTextArgs extends MorphToTextArgs {

		public MorphToBitmapTextArgs(String text, BitmapFontAssetModel font) {
			super(text, 32);
			this.font = font;
		}

		public BitmapFontAssetModel font;
	}

	protected static class MorphToSpriteArgs extends MorphToArgs {
		public IAssetKey asset;

		public MorphToSpriteArgs(IAssetKey asset) {
			super();
			this.asset = asset;
		}

	}

	@Override
	public final Object execute(ExecutionEvent event) throws ExecutionException {
		Object[] sel = ((IStructuredSelection) HandlerUtil.getCurrentSelection(event)).toArray();

		BitmapFontAssetModel font = null;
		IAssetKey texture = null;
		String text = null;

		CompositeOperation operations = new CompositeOperation();

		{
			List<String> beforeSelection = new ArrayList<>();

			for (Object elem : sel) {
				beforeSelection.add(((IObjectNode) elem).getModel().getId());
			}
			operations.add(new SelectOperation(beforeSelection));
		}

		{
			List<String> afterSelection = new ArrayList<>();

			for (Object elem : sel) {

				if (!(elem instanceof ISpriteNode)) {
					continue;
				}

				ISpriteNode node = (ISpriteNode) elem;
				BaseObjectModel model = node.getModel();

				if (ITextSpriteModel.class.isAssignableFrom(_morphToType)) {

					// convert to Text
					if (text == null) {
						if (model instanceof ITextSpriteModel) {
							text = ((ITextSpriteModel) model).getText();
						} else {
							TextDialog dlg = AddTextHandler.createTextDialog();
							if (dlg.open() == Window.OK) {
								text = dlg.getResult();
							} else {
								return null;
							}
						}
					}
				}

				if (_morphToType == BitmapTextModel.class) {

					// convert to BitmapText

					if (font == null) {
						BitmapTextFontDialog dlg = new BitmapTextFontDialog(HandlerUtil.getActiveShell(event));
						CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
						dlg.setProject(editor.getEditorInputFile().getProject());
						if (dlg.open() == Window.OK) {
							font = dlg.getSelectedFont();
						} else {
							return null;
						}
					}

					afterSelection.add(addMorph(operations, node, new MorphToBitmapTextArgs(text, font)));

				} else if (_morphToType == TextModel.class) {
					// convert to Text

					afterSelection
							.add(addMorph(operations, node, new MorphToTextArgs(text, TextModel.DEF_STYLE_FONT_SIZE)));

				} else if (model instanceof AssetSpriteModel<?> && !(model instanceof BitmapTextModel)) {

					// convert from sprite to sprite (excluding bitmap font)

					IAssetKey asset = ((AssetSpriteModel<?>) model).getAssetKey();

					if (asset != null) {
						afterSelection.add(addMorph(operations, node, new MorphToSpriteArgs(asset)));
					}

				} else {

					// convert from anything (text, bitmaps?) else to sprite

					if (texture == null) {
						SelectTextureDialog dlg = new SelectTextureDialog(HandlerUtil.getActiveShell(event),
								"Select Texture");
						CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
						dlg.setProject(editor.getEditorInputFile().getProject());
						if (dlg.open() == Window.OK) {
							texture = (IAssetKey) dlg.getSelection().getFirstElement();
						} else {
							return null;
						}
					}

					afterSelection.add(addMorph(operations, node, new MorphToSpriteArgs(texture)));
				}
			}

			operations.add(new SelectOperation(afterSelection));
		}

		CanvasEditor editor = (CanvasEditor) HandlerUtil.getActiveEditor(event);
		ObjectCanvas canvas = editor.getCanvas();

		canvas.getUpdateBehavior().executeOperations(operations);

		if (_morphToType == TextModel.class) {
			canvas.getSelectionBehavior().updateSelectedNodes_async();
		}

		editor.getSettingsPage().refresh();

		return null;
	}

	public Class<T> getMorphToType() {
		return _morphToType;
	}

	protected final String addMorph(CompositeOperation operations, ISpriteNode srcNode, MorphToArgs args) {
		// delete source
		operations.add(new DeleteNodeOperation(srcNode.getModel().getId()));

		// create morph
		GroupNode parent = srcNode.getGroup();
		BaseObjectModel dstModel = createMorphModel(srcNode, args, parent);

		@SuppressWarnings("unlikely-arg-type")
		int i = parent.getNode().getChildren().indexOf(srcNode);
		operations.add(new AddNodeOperation(dstModel.toJSON(false), i, dstModel.getX(), dstModel.getY(),
				parent.getModel().getId(), false));
		return dstModel.getId();
	}

	protected abstract T createMorphModel(ISpriteNode srcNode, MorphToArgs args, GroupNode parent);

}
