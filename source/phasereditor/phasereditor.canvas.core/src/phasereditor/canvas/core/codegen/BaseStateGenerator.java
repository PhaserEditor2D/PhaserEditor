// The MIT License (MIT)
//
// Copyright (c) 2015, 2017 Arian Fornaris
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
package phasereditor.canvas.core.codegen;

import org.eclipse.swt.graphics.RGB;

import phasereditor.canvas.core.BaseObjectModel;
import phasereditor.canvas.core.CanvasModel;
import phasereditor.canvas.core.PhysicsType;
import phasereditor.canvas.core.StateSettings;

/**
 * @author arian
 *
 */
public abstract class BaseStateGenerator extends JSLikeCodeGenerator {

	protected StateSettings _state;

	public BaseStateGenerator(CanvasModel model) {
		super(model);
		_state = model.getStateSettings();
	}

	protected void generatePreloaderStateCode() {
		if (_state.isPreloader()) {
			trim(() -> {
				line();
				super.generateObjectCreation();
			});

			trim(() -> {
				line();
				super.generatePublicFields();
			});

			trim(() -> {
				String id = _state.getPreloadSpriteId();
				if (id != null) {
					BaseObjectModel obj = _model.getWorld().findById(id);
					if (obj != null) {
						line();
						line("this.load.setPreloadSprite(" + getLocalVarName(obj) + ", "
								+ _state.getPreloadSprite_direction().ordinal() + ");");
					}
				}

			});
		}
	}

	@Override
	protected void generateObjectCreation() {
		if (_state.isPreloader()) {
			return;
		}

		super.generateObjectCreation();
	}

	@Override
	protected void generatePublicFields() {
		if (_state.isPreloader()) {
			return;
		}

		super.generatePublicFields();
	}

	protected void generateInitMethodBody() {
		trim(() -> {
			line();
			userCode(_settings.getUserCode().getState_init_before());
		});

		trim(() -> {
			line();
			StateSettings state = _model.getStateSettings();
			if (!StateSettings.SCALE_MODE_NO_SCALE.equals(state.getScaleMode())) {
				line("this.scale.scaleMode = Phaser.ScaleManager." + state.getScaleMode() + ";");
			}
			if (state.isPageAlignHorizontally()) {
				line("this.scale.pageAlignHorizontally = true;");
			}
			if (state.isPageAlignVertically()) {
				line("this.scale.pageAlignVertically = true;");
			}
			if (state.isRendererRoundPixels()) {
				line("this.game.renderer.renderSession.roundPixels = true;");
			}
			if (state.getPhysicsSystem() != PhysicsType.NONE) {
				line("this.physics.startSystem(Phaser.Physics." + state.getPhysicsSystem().name() + ");");
			}
			if (!state.getStageBackgroundColor().equals(new RGB(0, 0, 0))) {
				line("this.stage.backgroundColor = '" + getHexString(state.getStageBackgroundColor()) + "';");
			}
		});

		trim(() -> {
			line();
			userCode(_settings.getUserCode().getState_init_after());
		});
	}

}
