/**
 * @author       Richard Davey <rich@photonstorm.com>
 * @copyright    2020 Photon Storm Ltd.
 * @license      {@link https://opensource.org/licenses/MIT|MIT License}
 */

var Video = require('./Video');
var GameObjectFactory = require('../GameObjectFactory');

/**
 * Creates a new Video Game Object and adds it to the Scene.
 *
 * Note: This method will only be available if the Video Game Object has been built into Phaser.
 *
 * @method Phaser.GameObjects.GameObjectFactory#video
 * @since 3.20.0
 *
 * @param {number} x - The horizontal position of this Game Object in the world.
 * @param {number} y - The vertical position of this Game Object in the world.
 * @param {string} [key] - Optional key of the Video this Game Object will play, as stored in the Video Cache.
 *
 * @return {Phaser.GameObjects.Video} The Game Object that was created.
 */
GameObjectFactory.register('video', function (x, y, key)
{
    var video = new Video(this.scene, x, y, key);

    this.displayList.add(video);
    this.updateList.add(video);

    return video;
});

//  When registering a factory function 'this' refers to the GameObjectFactory context.
//
//  There are several properties available to use:
//
//  this.scene - a reference to the Scene that owns the GameObjectFactory
//  this.displayList - a reference to the Display List the Scene owns
//  this.updateList - a reference to the Update List the Scene owns
