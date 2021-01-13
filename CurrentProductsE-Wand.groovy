/**
 *  Current Products E-Wand
 *
 *  Copyright 2021 Tyler Kass
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
metadata {
	definition (name: "Current Products E-Wand", namespace: "currentproducts", author: "Tyler Kass") {
        capability "WindowShade"
	}
    
    preferences {
        input name: "preferredClosed", type: "enum", title: "Preferred Closed Position", description: "The direction in which 'Close' should move the blinds to.", required: true, defaultValue: "Up", options: ["Up", "Down"]
    }
    
	simulator {}
	tiles {}
}

//For later use
private getCLUSTER_WINDOW_COVERING() { 0xFC10 }
private getATTRIBUTE_POSITION_TILT() { 0x26 }
private getADDITIONAL_PARAMS() { [mfgCode:0x1263] }

// parse events into attributes
def parse(String description) {
	log.trace "Parsing '${description}'"

	String[] descArr = description.split(" ", 0)
	int len = descArr.length

	//TEST: If the length is not 14, the command is incorrect and could cause crashes.
    if (len != 14) {
    	log.info ("Command is not movement command.")
        return
    }

    String commandType = descArr[len-3]
    if("51".equals(commandType)) { // 0x51 signifies the packet is of type movement command
    	String payload = descArr[len-1]
        String position = payload.substring(2,4)
        int posInt = zigbee.convertHexToInt(position)
        
        if ("Down".equals(preferredClosed)) { // If the preferred direction is down, swap the position. (-25 should be 25)
            posInt = posInt * -1
        }

        if(posInt > 100) {
            posInt = negateHex(posInt) // Gets the negation of the hex value to get the negative int it represents
            if (posInt < -100) { posInt = -100 } //Cap value here
        	//log.debug "DEBUG: Setting tiltAngle to ${posInt}"
			sendEvent(name: "position", value: posInt)
        }
        else {
        	//log.debug "DEBUG: Setting tiltAngle to ${posInt}"
			sendEvent(name: "position", value: posInt)
        }
    }
}

// Sends angle to the position
def setPosition(data) {
    log.debug "Up or Down: ${preferredClosed}"
    if ("Down".equals(preferredClosed)) {
        data = data * -1
    }

	log.trace "Setting tilt to '${data}'"

	String hexData = formatHex(data)

    //DEBUG ///////////////////////////////
//    log.debug "${zigbee.command(getCLUSTER_WINDOW_COVERING(), getATTRIBUTE_POSITION_TILT(), hexData, getADDITIONAL_PARAMS())}"
	///////////////////////////////////////

    sendEvent(name: "tiltAngle", value: "${data}")

    zigbee.command(getCLUSTER_WINDOW_COVERING(), getATTRIBUTE_POSITION_TILT(), getADDITIONAL_PARAMS(), hexData)
}

def open() {
    log.trace "Opening"
    sendEvent(name: "position", value: 0)
	String hexData = formatHex(0)

    zigbee.command(getCLUSTER_WINDOW_COVERING(), getATTRIBUTE_POSITION_TILT(), getADDITIONAL_PARAMS(), hexData)
}

def close() {
    log.trace "Closing"
    value = "Down".equals(preferredClosed) ? -100 : 100
    sendEvent(name: "position", value: "${value}")
	String hexData = formatHex(value)

    zigbee.command(getCLUSTER_WINDOW_COVERING(), getATTRIBUTE_POSITION_TILT(), getADDITIONAL_PARAMS(), hexData)
}

def installed() {
	log.trace "Installed"
	initialize()
}

def updated() {
	log.trace "Updated"
	initialize()
}

// used for initializing the tilt angle so the UI element appears
private initialize() {
	log.trace "Initialize"
	//sendEvent(name: "Tilt Angle", value: 0)
    sendEvent(name: "position", value: 0)
}

// Takes the positive or negative position and converts it into an unsigned hex value that can be read by the E-Wand
private formatHex(data) {
	int intData = data.toInteger()
	String hexData = Integer.toHexString(intData)
    if (hexData.length() < 2) { hexData = "0" + hexData } //Pad single-digit values with a 0.
    hexData = hexData.substring(hexData.length()-2,hexData.length()).toUpperCase()
    return hexData
}

// Takes a value, converts it into hex, and finds the negation of that hex
private negateHex(data) {
	int intData = data.toInteger()
	String hexData = Integer.toHexString(intData * -1)
    hexData = hexData.substring(hexData.length()-2,hexData.length()).toUpperCase()
    int returnVal = zigbee.convertHexToInt(hexData) * -1
    return returnVal
}
