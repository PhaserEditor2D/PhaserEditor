// The MIT License (MIT)
//
// Copyright (c) 2015, 2019 Arian Fornaris
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
package phasereditor.inspect.core.jsdoc;

/**
 * @author arian
 *
 */
public interface IPhaserFullnames {

	// Animation

	String Animation_key = "Phaser.Types.Animations.Animation.key";
	String Animation_frameRate = "Phaser.Types.Animations.Animation.frameRate";
	String Animation_duration = "Phaser.Types.Animations.Animation.duration";
	String Animation_delay = "Phaser.Types.Animations.Animation.delay";
	String Animation_repeat = "Phaser.Types.Animations.Animation.repeat";
	String Animation_repeatDelay = "Phaser.Types.Animations.Animation.repeatDelay";
	String Animation_yoyo = "Phaser.Types.Animations.Animation.yoyo";
	String Animation_showOnStart = "Phaser.Types.Animations.Animation.showOnStart";
	String Animation_hideOnComplete = "Phaser.Types.Animations.Animation.hideOnComplete";
	String Animation_skipMissedFrames = "Phaser.Types.Animations.Animation.skipMissedFrames";

	String AnimationFrame_key = "Phaser.Types.Animations.AnimationFrame.key";
	String AnimationFrame_duration = "Phaser.Types.Animations.AnimationFrame.duration";

	// Loader

	String Load_animation_url = "Phaser.Loader.LoaderPlugin.animation(url)";
	String Load_animation_dataKey = "Phaser.Loader.LoaderPlugin.animation(dataKey)";

	String Load_atlas_textureURL = "Phaser.Loader.LoaderPlugin.atlas(textureURL)";
	String Load_atlas_atlasURL = "Phaser.Loader.LoaderPlugin.atlas(atlasURL)";
	String Load_atlas_normalMap = "Phaser.Types.Loader.FileTypes.AtlasJSONFileConfig.normalMap";

	String Load_audio_urls = "Phaser.Loader.LoaderPlugin.audio(urls)";

	String Load_audioSprite_jsonURL = "Phaser.Loader.LoaderPlugin.audioSprite(jsonURL)";
	String Load_audioSprite_audioURL = "Phaser.Loader.LoaderPlugin.audioSprite(audioURL)";

	String Load_bitmapFont_textureURL = "Phaser.Loader.LoaderPlugin.bitmapFont(textureURL)";
	String Load_bitmapFont_fontDataURL = "Phaser.Loader.LoaderPlugin.bitmapFont(fontDataURL)";
	String Load_bitmapFont_normalMap = "Phaser.Types.Loader.FileTypes.BitmapFontFileConfig.normalMap";

	String Load_htmlTexture_url = "Phaser.Loader.LoaderPlugin.htmlTexture(url)";
	String Load_htmlTexture_width = "Phaser.Loader.LoaderPlugin.htmlTexture(width)";
	String Load_htmlTexture_height = "Phaser.Loader.LoaderPlugin.htmlTexture(height)";

	String Load_image_url = "Phaser.Loader.LoaderPlugin.image(url)";
	String Load_image_normalMap = "Phaser.Types.Loader.FileTypes.ImageFileConfig.normalMap";

	String Load_multiatlas_atlasURL = "Phaser.Loader.LoaderPlugin.multiatlas(atlasURL)";
	String Load_multiatlas_path = "Phaser.Loader.LoaderPlugin.multiatlas(path)";

	String Load_scripts_url = "Phaser.Loader.LoaderPlugin.scripts(url)";

	String Load_plugin_url = "Phaser.Loader.LoaderPlugin.plugin(url)";
	String Load_plugin_start = "Phaser.Loader.LoaderPlugin.plugin(start)";
	String Load_plugin_mapping = "Phaser.Loader.LoaderPlugin.plugin(mapping)";

	String Load_scenePlugin_url = "Phaser.Loader.LoaderPlugin.scenePlugin(url)";
	String Load_scenePlugin_systemKey = "Phaser.Loader.LoaderPlugin.scenePlugin(systemKey)";
	String Load_scenePlugin_sceneKey = "Phaser.Loader.LoaderPlugin.scenePlugin(sceneKey)";
	
	String Load_spritesheet_url = "Phaser.Loader.LoaderPlugin.spritesheet(url)";
	String Load_spritesheet_normalMap = "Phaser.Types.Loader.FileTypes.SpriteSheetFileConfig.normalMap";
	String Load_spritesheet_frameWidth = "Phaser.Types.Loader.FileTypes.ImageFrameConfig.frameWidth";
	String Load_spritesheet_frameHeight = "Phaser.Types.Loader.FileTypes.ImageFrameConfig.frameHeight";
	String Load_spritesheet_startFrame = "Phaser.Types.Loader.FileTypes.ImageFrameConfig.startFrame";
	String Load_spritesheet_endFrame = "Phaser.Types.Loader.FileTypes.ImageFrameConfig.endFrame";
	String Load_spritesheet_spacing = "Phaser.Types.Loader.FileTypes.ImageFrameConfig.spacing";
	String Load_spritesheet_margin = "Phaser.Types.Loader.FileTypes.ImageFrameConfig.margin";
	
	String Load_svg_url = "Phaser.Loader.LoaderPlugin.svg(url)";
	
}
