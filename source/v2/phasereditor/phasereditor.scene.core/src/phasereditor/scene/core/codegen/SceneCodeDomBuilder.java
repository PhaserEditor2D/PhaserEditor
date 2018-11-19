// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.scene.core.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.core.AnimationsComponent;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.DynamicBitmapTextComponent;
import phasereditor.scene.core.DynamicBitmapTextModel;
import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.ImageModel;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TextualComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.core.codedom.AssignPropertyDom;
import phasereditor.scene.core.codedom.ClassDeclDom;
import phasereditor.scene.core.codedom.MethodCallDom;
import phasereditor.scene.core.codedom.MethodDeclDom;
import phasereditor.scene.core.codedom.RawCode;
import phasereditor.scene.core.codedom.UnitDom;

/**
 * @author arian
 *
 */
public class SceneCodeDomBuilder {

	private IFile _file;

	public SceneCodeDomBuilder(IFile file) {
		_file = file;
	}

	private static String varname(ObjectModel model) {

		var name = EditorComponent.get_editorName(model);

		var id = JSCodeUtils.id(name);

		return id;
	}

	public UnitDom build(SceneModel model) {

		var unit = new UnitDom();

		var clsName = _file.getFullPath().removeFileExtension().lastSegment();

		var clsDom = new ClassDeclDom(clsName);
		clsDom.setSuperClass("Phaser.Scene");

		var preloadDom = buildPreloadMethod(model);

		var createDom = buildCreateMethod(model);

		clsDom.getMembers().add(preloadDom);
		clsDom.getMembers().add(createDom);

		unit.getElements().add(clsDom);

		return unit;
	}

	private MethodDeclDom buildCreateMethod(SceneModel sceneModel) {
		var methodDecl = new MethodDeclDom("create");

		var worldModel = sceneModel.getDisplayList();
		var fieldModels = new ArrayList<ObjectModel>();

		for (var model : ParentComponent.get_children(worldModel)) {
			MethodCallDom methodCall = null;

			if (model instanceof TileSpriteModel) {

				methodCall = buildCreateTileSprite(methodDecl, (TileSpriteModel) model);

			} else if (model instanceof BitmapTextModel) {

				methodCall = buildCreateBitmapText(methodDecl, (BitmapTextModel) model);

			} else if (model instanceof SpriteModel) {

				methodCall = buildCreateSprite(methodDecl, (SpriteModel) model);

			} else if (model instanceof ImageModel) {

				methodCall = buildCreateImage(methodDecl, (ImageModel) model);

			}

			var assignToVar = false;

			if (EditorComponent.get_editorField(model)) {
				assignToVar = true;
				fieldModels.add(model);
			}

			if (model instanceof OriginComponent) {
				assignToVar = buildOriginProps(methodDecl, model) || assignToVar;
			}

			if (model instanceof TransformComponent) {
				assignToVar = buildTransformProps(methodDecl, model) || assignToVar;
			}

			if (model instanceof FlipComponent) {
				assignToVar = buildFlipProps(methodDecl, model) || assignToVar;
			}

			if (model instanceof BitmapTextComponent) {
				assignToVar = buildAlignProp(methodDecl, model) || assignToVar;
				assignToVar = buildLetterSpacingProp(methodDecl, model) || assignToVar;
			}

			if (model instanceof DynamicBitmapTextComponent) {
				assignToVar = buildDynamicBitmapTextProps(methodDecl, model) || assignToVar;
			}

			if (model instanceof AnimationsComponent) {
				assignToVar = buildAnimationsProps(methodDecl, model) || assignToVar;
			}

			if (model instanceof TileSpriteComponent) {
				assignToVar = buildTileSpriteProps(methodDecl, model) || assignToVar;
			}

			if (assignToVar && methodCall != null) {
				methodCall.setReturnToVar(varname(model));
			}

			methodDecl.getInstructions().add(new RawCode(""));

		}

		for (var model : fieldModels) {
			var local = varname(model);

			var fieldName = JSCodeUtils.fieldOf(local);

			var instr = new AssignPropertyDom(fieldName, "this");
			instr.value(local);

			methodDecl.getInstructions().add(instr);
		}

		{
			var userCode = sceneModel.getCreateUserCode();

			var before = userCode.getBeforeCode();

			if (before.length() > 0) {
				methodDecl.getInstructions().add(0, new RawCode(before));
			}

			var after = userCode.getAfterCode();

			if (after.length() > 0) {
				methodDecl.getInstructions().add(new RawCode(after));
			}
		}

		return methodDecl;
	}

	@SuppressWarnings("static-method")
	private boolean buildTileSpriteProps(MethodDeclDom methodDecl, ObjectModel model) {
		var assignToVar = false;

		var name = varname(model);

		// tilePositionX
		{
			var tilePositionX = TileSpriteComponent.get_tilePositionX(model);

			if (tilePositionX != TileSpriteComponent.tilePositionX_default) {

				assignToVar = true;

				var instr = new AssignPropertyDom("tilePositionX", name);

				instr.value(tilePositionX);

				methodDecl.getInstructions().add(instr);
			}
		}

		// tilePositionY
		{
			var tilePositionY = TileSpriteComponent.get_tilePositionY(model);

			if (tilePositionY != TileSpriteComponent.tilePositionY_default) {

				assignToVar = true;

				var instr = new AssignPropertyDom("tilePositionY", name);

				instr.value(tilePositionY);

				methodDecl.getInstructions().add(instr);
			}
		}

		// tileScaleX
		{
			var tileScaleX = TileSpriteComponent.get_tileScaleX(model);

			if (tileScaleX != TileSpriteComponent.tileScaleX_default) {

				assignToVar = true;

				var instr = new AssignPropertyDom("tileScaleX", name);

				instr.value(tileScaleX);

				methodDecl.getInstructions().add(instr);
			}
		}

		// tileScaleY
		{
			var tileScaleY = TileSpriteComponent.get_tileScaleY(model);

			if (tileScaleY != TileSpriteComponent.tileScaleY_default) {

				assignToVar = true;

				var instr = new AssignPropertyDom("tileScaleY", name);

				instr.value(tileScaleY);

				methodDecl.getInstructions().add(instr);
			}
		}

		return assignToVar;
	}

	@SuppressWarnings("static-method")
	private boolean buildAnimationsProps(MethodDeclDom methodDecl, ObjectModel model) {
		var key = AnimationsComponent.get_autoPlayAnimKey(model);

		if (key == null) {
			return false;
		}

		var name = varname(model);

		var instr = new MethodCallDom("play", name + ".anims");
		instr.argLiteral(key);

		methodDecl.getInstructions().add(instr);

		return true;
	}

	@SuppressWarnings("static-method")
	private boolean buildFlipProps(MethodDeclDom methodDecl, ObjectModel model) {

		var assignToVar = false;

		var name = varname(model);

		// flipX
		{
			var flipX = FlipComponent.get_flipX(model);

			if (flipX != FlipComponent.flipX_default) {

				assignToVar = true;

				var instr = new AssignPropertyDom("flipX", name);

				instr.value(flipX);

				methodDecl.getInstructions().add(instr);
			}
		}

		// flipY
		{
			var flipY = FlipComponent.get_flipY(model);

			if (flipY != FlipComponent.flipY_default) {

				assignToVar = true;

				var instr = new AssignPropertyDom("flipY", name);

				instr.value(flipY);

				methodDecl.getInstructions().add(instr);
			}
		}

		return assignToVar;
	}

	@SuppressWarnings("static-method")
	private boolean buildDynamicBitmapTextProps(MethodDeclDom methodDecl, ObjectModel model) {

		var assignToVar = false;

		var name = varname(model);

		// displayCallback
		{
			var displayCallback = DynamicBitmapTextComponent.get_displayCallback(model);

			if (displayCallback != null && displayCallback.trim().length() > 0) {

				assignToVar = true;

				var instr = new MethodCallDom("setDisplayCallback", name);

				instr.arg(displayCallback);

				methodDecl.getInstructions().add(instr);
			}
		}

		// crop size
		{
			var cropWidth = DynamicBitmapTextComponent.get_cropWidth(model);
			var cropHeight = DynamicBitmapTextComponent.get_cropHeight(model);

			// the text is not cropped if the widht or height are 0.

			if (cropWidth != DynamicBitmapTextComponent.cropWidth_default
					&& cropHeight != DynamicBitmapTextComponent.cropHeight_default) {

				assignToVar = true;

				var instr = new MethodCallDom("setSize", name);

				instr.arg(cropWidth);
				instr.arg(cropHeight);

				methodDecl.getInstructions().add(instr);
			}
		}

		// scroll X
		{
			var scrollX = DynamicBitmapTextComponent.get_scrollX(model);

			if (scrollX != DynamicBitmapTextComponent.scrollX_default) {

				assignToVar = true;

				var instr = new AssignPropertyDom("scrollX", name);

				instr.value(scrollX);

				methodDecl.getInstructions().add(instr);
			}
		}

		// scroll Y
		{
			var scrollY = DynamicBitmapTextComponent.get_scrollY(model);

			if (scrollY != DynamicBitmapTextComponent.scrollY_default) {

				assignToVar = true;

				var instr = new AssignPropertyDom("scrollY", name);

				instr.value(scrollY);

				methodDecl.getInstructions().add(instr);
			}
		}

		return assignToVar;
	}

	@SuppressWarnings("static-method")
	private boolean buildLetterSpacingProp(MethodDeclDom methodDecl, ObjectModel model) {
		var letterSpacing = BitmapTextComponent.get_letterSpacing(model);

		if (letterSpacing == BitmapTextComponent.letterSpacing_default) {
			return false;
		}

		var name = varname(model);

		var assign = new AssignPropertyDom("letterSpacing", name);

		assign.value(letterSpacing);

		methodDecl.getInstructions().add(assign);

		return true;
	}

	@SuppressWarnings("static-method")
	private boolean buildAlignProp(MethodDeclDom methodDecl, ObjectModel model) {
		var align = BitmapTextComponent.get_align(model);

		if (align == BitmapTextComponent.align_default) {
			return false;
		}

		var name = varname(model);

		var assign = new AssignPropertyDom("align", name);

		assign.value(align);

		methodDecl.getInstructions().add(assign);

		return true;
	}

	@SuppressWarnings("static-method")
	private MethodCallDom buildCreateBitmapText(MethodDeclDom methodDecl, BitmapTextModel model) {

		var methodName = model instanceof DynamicBitmapTextModel ? "dynamicBitmapText" : "bitmapText";

		var call = new MethodCallDom(methodName, "this.add");

		call.arg(TransformComponent.get_x(model));
		call.arg(TransformComponent.get_y(model));

		var asset = BitmapTextComponent.get_font(model);

		if (asset == null) {
			call.arg("null");
		} else {
			call.argLiteral(asset.getKey());
		}

		call.argLiteral(TextualComponent.get_text(model));
		call.arg(BitmapTextComponent.get_fontSize(model));
		call.arg(BitmapTextComponent.get_align(model));

		methodDecl.getInstructions().add(call);

		return call;
	}

	@SuppressWarnings("static-method")
	private boolean buildTransformProps(MethodDeclDom methodDecl, ObjectModel model) {

		var assignToVar = false;

		{
			var x = TransformComponent.get_scaleX(model);
			var y = TransformComponent.get_scaleY(model);

			if (x != TransformComponent.scaleX_default || y != TransformComponent.scaleY_default) {

				assignToVar = true;

				var name = varname(model);
				var call = new MethodCallDom("setScale", name);

				call.arg(TransformComponent.get_scaleX(model));
				call.arg(TransformComponent.get_scaleY(model));

				methodDecl.getInstructions().add(call);
			}
		}

		{

			var angle = TransformComponent.get_angle(model);

			if (angle != TransformComponent.angle_default) {

				assignToVar = true;

				var name = varname(model);
				var call = new MethodCallDom("setAngle", name);

				call.arg(angle);

				methodDecl.getInstructions().add(call);
			}
		}

		return assignToVar;
	}

	@SuppressWarnings("static-method")
	private boolean buildOriginProps(MethodDeclDom methodDecl, ObjectModel model) {

		var x = OriginComponent.get_originX(model);
		var y = OriginComponent.get_originY(model);

		var x_default = OriginComponent.originX_default(model);
		var y_default = OriginComponent.originY_default(model);

		if (x == x_default && y == y_default) {
			return false;
		}

		var name = varname(model);
		var call = new MethodCallDom("setOrigin", name);

		call.arg(OriginComponent.get_originX(model));
		call.arg(OriginComponent.get_originY(model));

		methodDecl.getInstructions().add(call);

		return true;
	}

	@SuppressWarnings("static-method")
	private MethodCallDom buildCreateSprite(MethodDeclDom methodDecl, SpriteModel model) {
		var call = new MethodCallDom("sprite", "this.add");

		call.arg(TransformComponent.get_x(model));
		call.arg(TransformComponent.get_y(model));

		var frame = TextureComponent.get_frame(model);

		buildTextureArguments(call, frame);

		methodDecl.getInstructions().add(call);

		return call;
	}

	@SuppressWarnings("static-method")
	private MethodCallDom buildCreateImage(MethodDeclDom methodDecl, ImageModel model) {
		var call = new MethodCallDom("image", "this.add");

		call.arg(TransformComponent.get_x(model));
		call.arg(TransformComponent.get_y(model));

		var frame = TextureComponent.get_frame(model);

		buildTextureArguments(call, frame);

		methodDecl.getInstructions().add(call);

		return call;
	}

	@SuppressWarnings("static-method")
	private MethodCallDom buildCreateTileSprite(MethodDeclDom methodDecl, TileSpriteModel model) {

		var call = new MethodCallDom("tileSprite", "this.add");

		call.arg(TransformComponent.get_x(model));
		call.arg(TransformComponent.get_y(model));

		call.arg(TileSpriteComponent.get_width(model));
		call.arg(TileSpriteComponent.get_height(model));

		var frame = TextureComponent.get_frame(model);

		buildTextureArguments(call, frame);

		methodDecl.getInstructions().add(call);

		return call;
	}

	private static void buildTextureArguments(MethodCallDom call, IAssetFrameModel frame) {
		var asset = frame.getAsset();

		if (asset instanceof ImageAssetModel) {
			call.argLiteral(asset.getKey());
		} else {
			call.argLiteral(asset.getKey());
			if (frame instanceof SpritesheetAssetModel.FrameModel) {
				call.arg(((SpritesheetAssetModel.FrameModel) frame).getIndex());
			} else {
				call.argLiteral(frame.getKey());
			}
		}
	}

	@SuppressWarnings("static-method")
	private MethodDeclDom buildPreloadMethod(SceneModel model) {

		var preloadDom = new MethodDeclDom("preload");

		Map<String, String[]> packSectionList = new HashMap<>();

		model.getDisplayList().visit(objModel -> {
			if (objModel instanceof TextureComponent) {
				var frame = TextureComponent.get_frame(objModel);
				if (frame != null) {

					var pack = ProjectCore.getAssetUrl(frame.getAsset().getPack().getFile());
					var section = frame.getAsset().getSection().getKey();

					packSectionList.put(section + "-" + pack, new String[] { section, pack });
				}
			}
		});

		for (var pair : packSectionList.values()) {

			var call = new MethodCallDom("pack", "this.load");

			call.argLiteral(pair[0]);
			call.argLiteral(pair[1]);

			preloadDom.getInstructions().add(call);

		}

		{
			var userCode = model.getPreloadUserCode();

			var before = userCode.getBeforeCode();

			if (before.length() > 0) {
				preloadDom.getInstructions().add(0, new RawCode(before));
			}

			var after = userCode.getAfterCode();

			if (after.length() > 0) {
				preloadDom.getInstructions().add(new RawCode(after));
			}
		}

		return preloadDom;
	}

}
