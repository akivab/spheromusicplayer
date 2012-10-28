package com.musicplayer.MusicPlayer;

import orbotix.robot.sensor.AccelerometerData;
import orbotix.robot.sensor.AttitudeSensor;

/**
 * Must hold the light away from you, so that the "back" is facing 
 * straight in front of you.
 * 
 * Negative to positive crossing is extremely important.
 * 
 * @author dlluncor
 *
 */
public class Helper {

	private boolean rollNegToPos = true;
	private int prevRoll;
	
	private int prevYaw;
	private int prevPitch;
	
	private double prevX;
	
	/**
	 * 
	 * Turn to the right is volume up.
	 * Turn to the left is volume down.
	 * 
	 * Tilt forward is go to next song.
	 * 
	 * Shuffle is shaking it.
	 * 
	 * Tilt backwards is pause or play song depending on its state.
	 * 
	 */
	
	public String listen(AttitudeSensor sensor, AccelerometerData accel) {
		double curX = accel.getFilteredAcceleration().x;
		double diffX = Math.abs(prevX - curX);
		// Don't affect volume or song if we are shaking it wildly.
		if (diffX > 1) {
			if (diffX > 2) {
				return "shuffle";
			}
			return "";
		}
		
		int rollDiff = Math.abs(sensor.roll - prevRoll);
		int pitchDiff = Math.abs(sensor.pitch - prevPitch) / 2;
		
		if (rollDiff > pitchDiff) {
		  prevPitch = sensor.pitch;
		  return controlVolume(sensor.roll);
		} else if (pitchDiff > rollDiff) {
		  prevRoll = sensor.roll;
		  return controlStartStop(sensor.pitch);
		}
		return "";
	}
	
	private String controlVolume(int currentRoll) {
		int diff = currentRoll - prevRoll;
		// Do nothing if we are moving just a little bit.
		if (Math.abs(diff) < 80) {
			return "";
		}
		
		// Which way in the track are we going.
		if (currentRoll > 80 && prevRoll < 80) {
			rollNegToPos = true;
		}
		if (currentRoll < 80 && prevRoll > 80) {
			rollNegToPos = false;
		}
		
		// Flip the direction when it gets high at that point.
//		if (currentRoll > 80 || currentRoll < -80) {
//			rollNegToPos = !rollNegToPos;
//		}
		
		int turnVolume = Math.abs(diff);
		prevRoll = currentRoll;
		if (diff > 0) {
			return volumeUp(turnVolume);
		} else {
			return volumeDown(turnVolume);
		}
		// if diff > 0 and we are going from negative to positive
		// turn volume up by diff amount.
		// else turn volume down.
	}
	
	private String volumeUp(int amount) {
		return "volup";
	}
	
	private String volumeDown(int amount) {
		return "voldown";
	}
	
	private String controlStartStop(int currentPitch) {
		int diff = currentPitch - prevPitch;
		// Do nothing if we are moving just a little bit.
		if (Math.abs(diff) < 100) {
			return "";
		}
		
		prevPitch = currentPitch;
		if (diff > 0) {
			return "next";
		} else {
			return "pause";
		}
	}
}