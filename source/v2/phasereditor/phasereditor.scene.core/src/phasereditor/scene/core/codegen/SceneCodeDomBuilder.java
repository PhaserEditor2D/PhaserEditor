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

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.core.resources.IFile;

import phasereditor.assetpack.core.AssetFinder;
import phasereditor.assetpack.core.AssetPackCore;
import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.core.AnimationsComponent;
import phasereditor.scene.core.BitmapTextComponent;
import phasereditor.scene.core.BitmapTextModel;
import phasereditor.scene.core.DynamicBitmapTextComponent;
import phasereditor.scene.core.DynamicBitmapTextModel;
import phasereditor.scene.core.FlipComponent;
import phasereditor.scene.core.GameObjectComponent;
import phasereditor.scene.core.GameObjectModel;
import phasereditor.scene.core.GroupModel;
import phasereditor.scene.core.ImageModel;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.SceneModel;
import phasereditor.scene.core.SceneModel.MethodContextType;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TextualComponent;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.core.VariableComponent;
import phasereditor.scene.core.VisibleComponent;
import phasereditor.scene.core.codedom.AssignPropertyDom;
import phasereditor.scene.core.codedom.ClassDeclDom;
import phasereditor.scene.core.codedom.MemberDeclDom;
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
	private AssetFinder _finder;

	public SceneCodeDomBuilder(IFile file) {
		_file = file;
		_finder = AssetPackCore.getAssetFinder(file.getProject());
	}

	private static String varname(ObjectModel model) {

		var name = VariableComponent.get_variableName(model);

		var id = JSCodeUtils.id(name);

		return id;
	}

	public UnitDom build(SceneModel model) {

		var methods = new ArrayList<MemberDeclDom>();

		if (model.isAutoLoadAssets()) {
			var preloadDom = buildPreloadMethod(model);
			methods.add(preloadDom);
		}

		var createDom = buildCreateMethod(model);
		methods.add(createDom);

		var unit = new UnitDom();

		if (model.isOnlyGenerateMethods()) {

			unit.getElements().addAll(methods);

		} else {

			var clsName = _file.getFullPath().removeFileExtension().lastSegment();

			var clsDom = new ClassDeclDom(clsName);

			clsDom.setSuperClass(model.getSuperClassName());

			{
				var key = model.getSceneKey();
				if (key.trim().length() > 0) {
					var ctrMethod = buildConstructorMethod(key);
					clsDom.getMembers().add(ctrMethod);
				}
			}

			clsDom.getMembers().addAll(methods);
			unit.getElements().add(clsDom);
		}

		return unit;
	}

	private static MethodDeclDom buildConstructorMethod(String sceneKey) {
		var methodDecl = new MethodDeclDom("constructor");

		var superCall = new MethodCallDom("super", null);
		superCall.argLiteral(sceneKey);

		methodDecl.getInstructions().add(superCall);

		return methodDecl;
	}

	private MethodDeclDom buildCreateMethod(SceneModel sceneModel) {
		var methodDecl = new MethodDeclDom(sceneModel.getCreateMethodName());

		var fieldModels = new ArrayList<ObjectModel>();

		var hasGroupSet = sceneModel.getGroupsModel().buildHasGroupSet();

		for (var model : sceneModel.getDisplayList().getChildren()) {
			MethodCallDom methodCall = null;

			if (model instanceof TileSpriteModel) {

				methodCall = buildCreateTileSprite(methodDecl, (TileSpriteModel) model, sceneModel);

			} else if (model instanceof BitmapTextModel) {

				methodCall = buildCreateBitmapText(methodDecl, (BitmapTextModel) model, sceneModel);

			} else if (model instanceof SpriteModel) {

				methodCall = buildCreateSprite(methodDecl, (SpriteModel) model, sceneModel);

			} else if (model instanceof ImageModel) {

				methodCall = buildCreateImage(methodDecl, (ImageModel) model, sceneModel);

			}

			var assignToVar = false;

			if (hasGroupSet.contains(model)) {
				assignToVar = true;
			}

			if (VariableComponent.get_variableField(model)) {
				assignToVar = true;
				fieldModels.add(model);
			}

			if (model instanceof GameObjectComponent) {
				assignToVar = buildNameProp(methodDecl, model) || assignToVar;

				assignToVar = buildSingleFlagProp(methodDecl, model, "active", GameObjectComponent::get_active,
						GameObjectComponent.active_default) || assignToVar;

				assignToVar = buildDataProp(methodDecl, model) || assignToVar;

			}

			if (model instanceof VisibleComponent) {
				assignToVar = buildSingleFlagProp(methodDecl, model, "visible", VisibleComponent::get_visible,
						VisibleComponent.visible_default) || assignToVar;
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

			// do this always at the end!
			if (GameObjectComponent.is(model)) {
				assignToVar = buildObjectBuildCall(methodDecl, model) || assignToVar;
			}

			{

				var isObjectContext = sceneModel.getMethodContextType() == MethodContextType.OBJECT;

				if (methodCall != null) {

					var varname = varname(model);

					if (assignToVar || isObjectContext) {
						methodCall.setReturnToVar(varname);
					}

					if (isObjectContext) {
						var call = new MethodCallDom("add", "this");
						call.arg(varname);
						methodDecl.getInstructions().add(call);
					}
				}

			}

			// add a new line
			methodDecl.getInstructions().add(new RawCode(""));
		}

		buildGroups(sceneModel, methodDecl);

		for (var model : fieldModels) {
			var local = varname(model);

			var fieldName = JSCodeUtils.fieldOf(local);

			var instr = new AssignPropertyDom(fieldName, "this");
			instr.value(local);

			methodDecl.getInstructions().add(instr);
		}

		if (sceneModel.isGenerateMethodEvents()) {
			methodDecl.getInstructions().add(0, new RawCode("\nthis.events.emit('-precreate');\n "));
			methodDecl.getInstructions().add(new RawCode("\nthis.events.emit('-postcreate');\n "));
		}

		return methodDecl;
	}

	private static boolean buildObjectBuildCall(MethodDeclDom methodDecl, ObjectModel model) {

		if (GameObjectComponent.get_objectBuild(model)) {
			var name = varname(model);
			var call = new MethodCallDom("build", name);
			methodDecl.getInstructions().add(call);
			return true;
		}

		return false;
	}

	private static boolean buildNameProp(MethodDeclDom methodDecl, ObjectModel model) {

		var useName = GameObjectComponent.get_useName(model);

		if (useName) {
			var name = varname(model);
			var prop = new AssignPropertyDom("name", name);
			prop.valueLiteral(name);
			methodDecl.getInstructions().add(prop);
			return true;
		}

		return false;
	}

	private static boolean buildDataProp(MethodDeclDom methodDecl, ObjectModel model) {

		var json = GameObjectComponent.get_data(model);

		if (json == null || json.keySet().isEmpty()) {
			return false;
		}

		var name = varname(model);

		for (var key : json.keySet()) {
			var value = json.getString(key);
			var call = new MethodCallDom("setData", name);
			call.argLiteral(key);
			call.arg(value);

			methodDecl.getInstructions().add(call);
		}

		return true;
	}

	@SuppressWarnings("static-method")
	private boolean buildSingleFlagProp(MethodDeclDom methodDecl, ObjectModel model, String propName,
			Function<ObjectModel, Boolean> get, boolean defaultValue) {
		var assignToVar = false;

		var name = varname(model);

		var flag = get.apply(model).booleanValue();

		if (flag != defaultValue) {

			assignToVar = true;

			var instr = new AssignPropertyDom(propName, name);

			instr.value(flag);

			methodDecl.getInstructions().add(instr);
		}

		return assignToVar;
	}

	private void buildGroups(SceneModel sceneModel, MethodDeclDom methodDecl) {

		var instructions = methodDecl.getInstructions();

		var len = instructions.size();

		for (var group : sceneModel.getGroupsModel().getGroups()) {
			var groupName = varname(group);

			var methodCall = buildCreateGroup(methodDecl, group, sceneModel);

			if (VariableComponent.get_variableField(group)) {
				var fieldName = JSCodeUtils.fieldOf(groupName);
				methodCall.setReturnToVar("this." + fieldName);
				methodCall.setDeclareReturnToVar(false);
			} else {
				methodCall.setReturnToVar(groupName);
			}
		}

		if (instructions.size() > len) {
			instructions.add(new RawCode(""));
		}
	}

	@SuppressWarnings("static-method")
	private MethodCallDom buildCreateGroup(MethodDeclDom methodDecl, GroupModel group, SceneModel sceneModel) {
		var call = new MethodCallDom("group", getObjectFactoryPath(sceneModel));

		var array = group.getChildren().stream().map(model -> varname(model)).collect(joining(", "));
		call.arg("[ " + array + " ]");
		methodDecl.getInstructions().add(call);

		return call;
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

	private MethodCallDom buildCreateBitmapText(MethodDeclDom methodDecl, BitmapTextModel model,
			SceneModel sceneModel) {

		var methodName = model instanceof DynamicBitmapTextModel ? "dynamicBitmapText" : "bitmapText";

		var call = new MethodCallDom(getObjectFactoryMethod(methodName, model), getObjectFactoryPath(sceneModel));

		call.arg(TransformComponent.get_x(model));
		call.arg(TransformComponent.get_y(model));

		var asset = BitmapTextComponent.utils_getFont(model, _finder);

		if (asset == null) {
			call.arg("null");
		} else {
			call.argLiteral(asset.getKey());
		}

		model.updateSizeFromBitmapFont(_finder);

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

	private MethodCallDom buildCreateSprite(MethodDeclDom methodDecl, SpriteModel model, SceneModel sceneModel) {
		var call = new MethodCallDom(getObjectFactoryMethod("sprite", model), getObjectFactoryPath(sceneModel));

		call.arg(TransformComponent.get_x(model));
		call.arg(TransformComponent.get_y(model));

		var frame = getTexture(model);

		buildTextureArguments(call, frame);

		methodDecl.getInstructions().add(call);

		return call;
	}

	private static String getObjectFactoryPath(SceneModel sceneModel) {
		switch (sceneModel.getMethodContextType()) {
		case SCENE:
			return "this.add";
		case OBJECT:
			return "this.scene.add";
		default:
			break;
		}
		return "this.add";
	}

	private MethodCallDom buildCreateImage(MethodDeclDom methodDecl, ImageModel model, SceneModel sceneModel) {
		var call = new MethodCallDom(getObjectFactoryMethod("image", model), getObjectFactoryPath(sceneModel));

		call.arg(TransformComponent.get_x(model));
		call.arg(TransformComponent.get_y(model));

		var frame = getTexture(model);

		buildTextureArguments(call, frame);

		methodDecl.getInstructions().add(call);

		return call;
	}

	private MethodCallDom buildCreateTileSprite(MethodDeclDom methodDecl, TileSpriteModel model,
			SceneModel sceneModel) {

		var call = new MethodCallDom(getObjectFactoryMethod("tileSprite", model), getObjectFactoryPath(sceneModel));

		call.arg(TransformComponent.get_x(model));
		call.arg(TransformComponent.get_y(model));

		call.arg(TileSpriteComponent.get_width(model));
		call.arg(TileSpriteComponent.get_height(model));

		var frame = getTexture(model);

		buildTextureArguments(call, frame);

		methodDecl.getInstructions().add(call);

		return call;
	}

	private static String getObjectFactoryMethod(String defaultName, GameObjectModel model) {

		var factory = GameObjectComponent.get_objectFactory(model);

		if (factory.trim().length() > 0) {
			return factory.trim();
		}

		return defaultName;
	}

	private IAssetFrameModel getTexture(ObjectModel model) {
		var frame = TextureComponent.utils_getTexture(model, _finder);

		if (frame == null) {
			// TODO: we should not generate code if there is any error, so it is missing a
			// previous validation
			throw new RuntimeException("Texture not found (" + TextureComponent.get_textureKey(model) + ","
					+ TextureComponent.get_textureFrame(model) + ")");
		}

		return frame;
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

	private MethodDeclDom buildPreloadMethod(SceneModel model) {

		var preloadDom = new MethodDeclDom(model.getPreloadMethodName());

		Map<String, String[]> packSectionList = new HashMap<>();

		model.getDisplayList().visit(objModel -> {
			if (objModel instanceof TextureComponent) {

				var frame = getTexture(objModel);

				var pack = ProjectCore.getAssetUrl(frame.getAsset().getPack().getFile());
				var section = frame.getAsset().getSection().getKey();

				packSectionList.put(section + "-" + pack, new String[] { section, pack });
			}
		});

		for (var pair : packSectionList.values()) {

			var call = new MethodCallDom("pack", "this.load");

			call.argLiteral(pair[0]);
			call.argLiteral(pair[1]);

			preloadDom.getInstructions().add(call);

		}

		if (model.isGenerateMethodEvents()) {
			preloadDom.getInstructions().add(0, new RawCode("\nthis.events.emit('-prepreload');\n "));
			preloadDom.getInstructions().add(new RawCode("\nthis.events.emit('-postpreload');\n "));
		}

		return preloadDom;
	}

}
