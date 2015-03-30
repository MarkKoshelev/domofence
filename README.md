This Android app let's you create a Geofence to switch a 'Dummy device' on your domoticz server.

## On your Domoticz server:
### Create a virtual switch DomoFence will trigger

Go to 'Setup -> Hardware' and add a 'Dummy device'

Go to 'Switches' and add a 'Manual light/switch'

Select the right 'Hardware' and 'On/Off' the rest doesn't matter.

Add the device

Now go to 'Setup -> Devices' and note the 'Idx' of the switch you just created.

### Secure your Domoticz server with a username and password

Go to 'Setup -> Settings'

At 'Website protection' fill in a username, password and select 'Basic-Auth'

Now Save your settings

### Open and port on your router and pass it to your internal domoticz server (usually through NAT)

Based on: https://github.com/googlesamples/android-play-location/tree/master/Geofencing

<div>Icon made by <a href="http://www.icons8.com" title="Icons8">Icons8</a> from <a href="http://www.flaticon.com" title="Flaticon">www.flaticon.com</a> is licensed under <a href="http://creativecommons.org/licenses/by/3.0/" title="Creative Commons BY 3.0">CC BY 3.0</a></div>
