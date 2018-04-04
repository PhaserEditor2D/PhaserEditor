// The MIT License (MIT)
//
// Copyright (c) 2015, 2016 Arian Fornaris
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
package phasereditor.canvas.core;

import org.json.JSONObject;

/**
 * @author arian
 *
 */
public abstract class ArcadeBodyModel extends BodyModel {
	private double _offsetX;
	private double _offsetY;
	private double _mass = 1;
	private boolean _moves = true;
	private boolean _immovable = false;
	private boolean _collideWorldBounds = false;
	private boolean _allowRotation = true;
	private boolean _allowGravity = true;
	private double _bounceX = 0;
	private double _bounceY = 0;
	private double _velocityX = 0;
	private double _velocityY = 0;
	private double _maxVelocityX = 10_000;
	private double _maxVelocityY = 10_000;
	private double _accelerationX = 0;
	private double _accelerationY = 0;
	private double _dragX = 0;
	private double _dragY = 0;
	private double _gravityX = 0;
	private double _gravityY = 0;
	private double _frictionX = 1;
	private double _frictionY = 0;
	private double _angularVelocity = 0;
	private double _maxAngular = 1_000;
	private double _angularAcceleration = 0;
	private double _angularDrag = 0;
	private boolean _checkCollisionNone = false;
	private boolean _checkCollisionUp = true;
	private boolean _checkCollisionDown = true;
	private boolean _checkCollisionLeft = true;
	private boolean _checkCollisionRight = true;
	private boolean _skipQuadTree = false;

	public ArcadeBodyModel() {
		_offsetX = 0;
		_offsetY = 0;
	}

	public double getOffsetX() {
		return _offsetX;
	}

	public void setOffsetX(double offsetX) {
		_offsetX = offsetX;
	}

	public double getOffsetY() {
		return _offsetY;
	}

	public void setOffsetY(double offsetY) {
		_offsetY = offsetY;
	}

	public double getMass() {
		return _mass;
	}

	public void setMass(double mass) {
		_mass = mass;
	}

	public boolean isMoves() {
		return _moves;
	}

	public void setMoves(boolean moves) {
		_moves = moves;
	}

	public boolean isImmovable() {
		return _immovable;
	}

	public void setImmovable(boolean immovable) {
		_immovable = immovable;
	}

	public boolean isCollideWorldBounds() {
		return _collideWorldBounds;
	}

	public void setCollideWorldBounds(boolean collideWorldBounds) {
		_collideWorldBounds = collideWorldBounds;
	}

	public boolean isAllowRotation() {
		return _allowRotation;
	}

	public void setAllowRotation(boolean allowRotation) {
		_allowRotation = allowRotation;
	}

	public boolean isAllowGravity() {
		return _allowGravity;
	}

	public void setAllowGravity(boolean allowGravity) {
		_allowGravity = allowGravity;
	}

	public double getBounceX() {
		return _bounceX;
	}

	public void setBounceX(double bounceX) {
		_bounceX = bounceX;
	}

	public double getBounceY() {
		return _bounceY;
	}

	public void setBounceY(double bounceY) {
		_bounceY = bounceY;
	}

	public double getVelocityX() {
		return _velocityX;
	}

	public void setVelocityX(double velocityX) {
		_velocityX = velocityX;
	}

	public double getVelocityY() {
		return _velocityY;
	}

	public void setVelocityY(double velocityY) {
		_velocityY = velocityY;
	}

	public double getMaxVelocityX() {
		return _maxVelocityX;
	}

	public void setMaxVelocityX(double maxVelocityX) {
		_maxVelocityX = maxVelocityX;
	}

	public double getMaxVelocityY() {
		return _maxVelocityY;
	}

	public void setMaxVelocityY(double maxVelocityY) {
		_maxVelocityY = maxVelocityY;
	}

	public double getAccelerationX() {
		return _accelerationX;
	}

	public void setAccelerationX(double accelerationX) {
		_accelerationX = accelerationX;
	}

	public double getAccelerationY() {
		return _accelerationY;
	}

	public void setAccelerationY(double accelerationY) {
		_accelerationY = accelerationY;
	}

	public double getDragX() {
		return _dragX;
	}

	public void setDragX(double dragX) {
		_dragX = dragX;
	}

	public double getDragY() {
		return _dragY;
	}

	public void setDragY(double dragY) {
		_dragY = dragY;
	}

	public double getGravityX() {
		return _gravityX;
	}

	public void setGravityX(double gravityX) {
		_gravityX = gravityX;
	}

	public double getGravityY() {
		return _gravityY;
	}

	public void setGravityY(double gravityY) {
		_gravityY = gravityY;
	}

	public double getFrictionX() {
		return _frictionX;
	}

	public void setFrictionX(double frictionX) {
		_frictionX = frictionX;
	}

	public double getFrictionY() {
		return _frictionY;
	}

	public void setFrictionY(double frictionY) {
		_frictionY = frictionY;
	}

	public double getAngularVelocity() {
		return _angularVelocity;
	}

	public void setAngularVelocity(double angularVelocity) {
		_angularVelocity = angularVelocity;
	}

	public double getMaxAngular() {
		return _maxAngular;
	}

	public void setMaxAngular(double maxAngular) {
		_maxAngular = maxAngular;
	}

	public double getAngularAcceleration() {
		return _angularAcceleration;
	}

	public void setAngularAcceleration(double angularAcceleration) {
		_angularAcceleration = angularAcceleration;
	}

	public double getAngularDrag() {
		return _angularDrag;
	}

	public void setAngularDrag(double angularDrag) {
		_angularDrag = angularDrag;
	}

	public boolean isCheckCollisionNone() {
		return _checkCollisionNone;
	}

	public void setCheckCollisionNone(boolean checkCollisionNone) {
		_checkCollisionNone = checkCollisionNone;
	}

	public boolean isCheckCollisionUp() {
		return _checkCollisionUp;
	}

	public void setCheckCollisionUp(boolean checkCollisionUp) {
		_checkCollisionUp = checkCollisionUp;
	}

	public boolean isCheckCollisionDown() {
		return _checkCollisionDown;
	}

	public void setCheckCollisionDown(boolean checkCollisionDown) {
		_checkCollisionDown = checkCollisionDown;
	}

	public boolean isCheckCollisionLeft() {
		return _checkCollisionLeft;
	}

	public void setCheckCollisionLeft(boolean checkCollisionLeft) {
		_checkCollisionLeft = checkCollisionLeft;
	}

	public boolean isCheckCollisionRight() {
		return _checkCollisionRight;
	}

	public void setCheckCollisionRight(boolean checkCollisionRight) {
		_checkCollisionRight = checkCollisionRight;
	}

	public boolean isSkipQuadTree() {
		return _skipQuadTree;
	}

	public void setSkipQuadTree(boolean skipQuadTree) {
		_skipQuadTree = skipQuadTree;
	}

	@Override
	protected void writeJSON(JSONObject data) {
		data.put("offsetX", _offsetX, 0);
		data.put("offsetY", _offsetY, 0);
		data.put("mass", _mass, 1);
		data.put("moves", _moves, true);
		data.put("immovable", _immovable, false);
		data.put("collideWorldBounds", _collideWorldBounds, false);
		data.put("allowRotation", _allowRotation, true);
		data.put("allowGravity", _allowGravity, true);
		data.put("bounceX", _bounceX, 0);
		data.put("bounceY", _bounceY, 0);
		data.put("velocityX", _velocityX, 0);
		data.put("velocityY", _velocityY, 0);
		data.put("maxVelocityX", _maxVelocityX, 10_000);
		data.put("maxVelocityY", _maxVelocityY, 10_000);
		data.put("accelerationX", _accelerationX, 0);
		data.put("accelerationY", _accelerationY, 0);
		data.put("dragX", _dragX, 0);
		data.put("dragY", _dragY, 0);
		data.put("gravityX", _gravityX, 0);
		data.put("gravityY", _gravityY, 0);
		data.put("frictionX", _frictionX, 1);
		data.put("frictionY", _frictionY, 0);
		data.put("angularVelocity", _angularVelocity, 0);
		data.put("maxAngular", _maxAngular, 1_000);
		data.put("angularAcceleration", _angularAcceleration, 0);
		data.put("angularDrag", _angularDrag, 0);
		data.put("checkCollisionNone", _checkCollisionNone, false);
		data.put("checkCollisionUp", _checkCollisionUp, true);
		data.put("checkCollisionDown", _checkCollisionDown, true);
		data.put("checkCollisionLeft", _checkCollisionLeft, true);
		data.put("checkCollisionRight", _checkCollisionRight, true);
		data.put("skipQuadTree", _skipQuadTree, false);
	}

	@Override
	public void readJSON(JSONObject data) {
		_offsetX = data.optDouble("offsetX", 0);
		_offsetY = data.optDouble("offsetY", 0);
		_mass = data.optDouble("mass", 1);
		_moves = data.optBoolean("moves", true);
		_immovable = data.optBoolean("immovable", false);
		_collideWorldBounds = data.optBoolean("collideWorldBounds", false);
		_allowRotation = data.optBoolean("allowRotation", true);
		_allowGravity = data.optBoolean("allowGravity", true);
		_bounceX = data.optDouble("bounceX", 0);
		_bounceY = data.optDouble("bounceY", 0);
		_velocityX = data.optDouble("velocityX", 0);
		_velocityY = data.optDouble("velocityY", 0);
		_maxVelocityX = data.optDouble("maxVelocityX", 10_000);
		_maxVelocityY = data.optDouble("maxVelocityY", 10_000);
		_accelerationX = data.optDouble("accelerationX", 0);
		_accelerationY = data.optDouble("accelerationY", 0);
		_dragX = data.optDouble("dragX", 0);
		_dragY = data.optDouble("dragY", 0);
		_gravityX = data.optDouble("gravityX", 0);
		_gravityY = data.optDouble("gravityY", 0);
		_frictionX = data.optDouble("frictionX", 1);
		_frictionY = data.optDouble("frictionY", 0);
		_angularVelocity = data.optDouble("angularVelocity", 0);
		_maxAngular = data.optDouble("maxAngular", 1_000);
		_angularAcceleration = data.optDouble("angularAcceleration", 0);
		_angularDrag = data.optDouble("angularDrag", 0);
		_checkCollisionNone = data.optBoolean("checkCollisionNone", false);
		_checkCollisionUp = data.optBoolean("checkCollisionUp", true);
		_checkCollisionDown = data.optBoolean("checkCollisionDown", true);
		_checkCollisionLeft = data.optBoolean("checkCollisionLeft", true);
		_checkCollisionRight = data.optBoolean("checkCollisionRight", true);
		_skipQuadTree = data.optBoolean("skipQuadTree", false);
	}
}
