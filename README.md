The GameAnalytics SDK for Android
=================================

This is the Android wrapper for GameAnalytics. It allows you to get analytics data from your Android game to your GameAnalytics account with the minimum of effort!

**Changelog**

**1.14.5**
- Allow for preview builds of Android with single letter codes. e.g. "L".

**1.14.4**
- As of 01/08/14 all new apps/updates to Google Play must use the Android advertising ID if available. SDK updated accordingly. You may notice strange numbers over a short period as users are transferred from one ID to the other.

**1.14.3**
- Updated to use application context.
- This should solve pre-existing issue of NullPointerException on access to Google Advertising ID preference.
- Improved algorithm for determining ID to use.

**1.14.2**
- Switched to static Database Helper to avoid recently introduced bug: "Illegal State Exception - attempt to re-open an already-closed object".
- Tweaked error response handling to handle errors without proper response codes.
- Fixed NullPointerException on access to Google Advertising ID preference.

**1.14.1**
- Fixed bug causing SQLiteDatabaseLockedException
- Fixed bug causing following error: "Warning: trying to fill in events with no user id but user id is still null."

**1.14.0**
- Added support for [Android Advertising ID](https://developer.android.com/google/play-services/id.html)
- Existing users will still be tracked with existing ID
- Users are now able to opt-out of analytics altogether on 4.4 Kitkat and above
- Including the Google Play Services library is compulsory from v1.14.0, see additional setup step below for tips.

**1.13.1**

- Changed format of os_major from "Android x.y" to "x.y"
- Changed format of os_minor from "Android x.y.z" to "x.y.z"
- Renamed all .jar files to consitent x.y.z. format for SDK version number

**1.13.0**

- The deleteSendEvents() method is now synchronized to stop simultaneous database access
- The custom user ID is no longer overwritten in the initialise() method
- Log info to console when event database is full (verbose mode only)
- Correct documentation setTimeInterval > setSendEventsInterval

**1.12.0**

- Updated SDK version format

**1.11.0**

- Added support for the error event category
- Added the clearDatabase() method which allows the forced-cleaning of cached events locally

##How it works - a brief description!

Every time a new event is created, the details are stored in a local SQLite database. In addition, a 'BatchThread' is started if one does not already exist. The 'BatchThread' waits for a specific interval and checks whether an internet connection is available. If so, it takes all the event info collected so far, puts them into JSON arrays and sends them off to the Game Analytics server.

If no internet connection is available then the thread will quietly poll the connection until it is resumed. As the events are stored in an SQLite database, your event information is safe even if the user resets their device before the connection is restored.

**Step 1 - Getting the code!**

You can get a copy of the GameAnalytics Android wrapper by either:

- Forking/cloning using Git (all you GitHub afficianados will already know how to do this!)
- Downloading directly as a .zip file using the button near the top of the screen

**Step 2 - Importing into your project**

There are two ways to get the wrapper into your application.

1. Using a library project:
	* Import the Game Analytics Wrapper project folder into Eclipse using File -> Import -> Existing Android Code Into Workspace.
	* Right click on the newly imported project and click Properties -> Android -> check 'Is Library'
	* Right click on your application that you want to use Game Analytics with and click Properties -> Android -> Under libraries click 'add' and select the Game-Analytics-Wrapper project.

2. Using JARs:
	* Copy the game-analytics-wrapper-vX.X.X.jar from the releases folder into the lib folder of your Android project.
	* Copy the gson-X.X.X.jar from the libs folder into the lib folder of your Android project
	* *Note: the jar may not be as up-to-date as the code on GitHub. Also you will not have access to the javadoc markup by going this route, whereas if you use the library project you will.*

**Step 3 - Google Play Services**

- From v1.14.0 onwards, the Google Play Services library needs to be included in your project. This is to allow Game Analytics to track the official Android Advertising ID. But don't worry, your game will still be playable on devices without Google Play Services so your users will not be inconvenienced.
- To include Google Play Services follow the instructions [here](http://developer.android.com/google/play-services/setup.html).
- If you are using method 1. Using a library project, you will need to include the Google Play Services library as a library project for the Game Analytics libary project. I.E. Your application will import the Game Analytics Library and then the Game Analytics Library will import the Google Play Game Services libary.
- If you are using method 2. Using JARs, then you will need include the Google Play Services Library directly in your own application project, in addition to the game-analytics-wrapper-vX.X.X.jar and gson-X.X.X.jar files.
- Finally, you need to include the following tag in your Android Manifest.xml inside the Application tags:
	
```java
<!-- FOR GOOGLE PLAY SERVICES AND GAME ANALYTICS -->
 <meta-data
            android:name="com.google.android.gms.version"
            android:value="@integer/google_play_services_version" />
```
	
**Step 4 - Basic setup**

If you haven't already, sign up for an account at www.gameanalytics.com. Make sure you write down your GAME KEY and SECRET KEY somewhere safe.

Initialise the wrapper in the entry activity of your application's onCreate() method:

```java

@Override
public void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
...

// Set-up game analytics
GameAnalytics.initialise(this, SECRET_KEY, GAME_KEY);

...
}

```

To ensure proper session logging put the following code in every activity's onResume() and onPause() method:

```java

@Override
public void onResume() {
super.onResume();
GameAnalytics.startSession(this);
}

@Override
public void onPause() {
super.onPause();
GameAnalytics.stopSession();
}

```

Finally, add the following two permissions to your AndroidManifest.xml (if you don't have them there already).

```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />

```

That�s it! Your basic Game Analytics functionality is set up.

## Logging custom events

You can read all about how the event logging system works with Game Analytics [here](http://support.gameanalytics.com/entries/22645043-Event-categories).

To log the different types of events you can use the following methods:

```java

GameAnalytics.newDesignEvent(eventId, value, area, x, y, z);
GameAnalytics.newBusinessEvent(eventId, currency, amount, area, x, y, z);
GameAnalytics.newUserEvent(eventId, gender, birthYear, friendCount, area, x, y, z);
GameAnalytics.newQualityEvent(eventId, message, area, x, y, z);

```

Use colons within the eventId string to denote subtypes. The area, x, y and z parameters are optional. If the area parameter is omitted then it defualts to the class name of the current activity.

## Extra features

The Android wrapper has a load of extra features to save you the work of coding them yourself. Here's a summary:

**Log Unhandled Exceptions** - Use the following code to automatically log unhandled exceptions:

    GameAnalytics.logUnhandledExceptions();

You can view them by going to the Quality tab of the Game Analytics dashboard and clicking on the EventId called �Exceptions�. To view the stack traces, simply click on the individual names of each exception.

**Log average FPS** - Add the following line of code to somewhere in your draw loop to automatically logFPS:

    GameAnalytics.logFPS()

If you are using OpenGL then this would be the onDrawFrame() method of your Renderer subclass. To emphasise, *this method should be called ON EVERY FRAME.*

To actually send the logged date to the server you must call stopLoggingFPS(). The recommended approach is to call it in the onPause() method of your activity so that it averages the FPS over the entire gameplay session. However your application may have alternative needs (for example if your draw routine does not have the same lifecycle as your activity), therefore stopLoggingFPS() can be called any time after logFPS() is first called. It will average the FPS over the intervening period.

Check out the DesignEventsActivity in the demo application (see below) if you are having trouble getting this working.

**Customise Intervals**

By default the Game Analytics Android Wrapper will batch together multiple events and only send them to the server every 20 seconds. You can change this interval using:

    GameAnalytics.setSendEventsInterval(int millis);

When no internet connection is available, the wrapper will poll the network connection every minute to see if it has been restored. You can change this interval using:

    GameAnalytics.setNetworkPollInterval(int millis);

After an activity calls stopSession() during its onPause() method, a session time out is started. If another activity calls onResume() and therefore startSession() within this time out interval then the same gaming session is continued instead of starting a new one. By default this time out interval is 10 seconds. You can change this interval using:

    GameAnalytics.setSessionTimeOut(int millis);

**Custom log level** - By default the Game Analytics wrapper will only post to the Android operating system log upon a warning or error. However to help during development it is recommended that you set the log level to VERBOSE with the following line of code:

    GameAnalytics.setDebugLogLevel(GameAnalytics.VERBOSE);

This will post logs every time an event is created and batched off to the server. *Place this before you call initialise() to ensure you see all logs.*

**Local Caching** - By default the Game Analytics wrapper will cache events locally when an internet connection is not available. The events will be sent once the connection is restored. Use the following code to disable this behaviour, events will be discarded if an internet connection is unavailable:

    GameAnalytics.setLocalCaching(false);

**Automatic Batching** - By default the Game Analytics wrapper will send events to the server in batches after a specific time interval. Use the following code to disable this behaviour:

    GameAnalytics.setAutoBatch(false);

... and use the following to send events to the GA server manually.

    GameAnalytics.manualBatch();

**Event capacity** - By default the local database will hold an unlimited number of events between sending batches to the server. Use the following code to set a specific event capacity. Additional events will be discarded:

    GameAnalytics.setMaximumEventStorage(capacity);

## Tips and tricks

**Parent Activity** - Use a parent class that extends Activity to remove the need to put startSession() and stopSession() in every activity's onPause and onResume() methods. The following example is taken from the demo application:

```java

public abstract class GameAnalyticsActivity extends Activity {

	@Override
	public void onResume() {
		super.onResume();
		GameAnalytics.startSession(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		GameAnalytics.stopSession();
	}
}

```

Then by making all your activities extend GameAnalyticsActivity you can ensure sessions are tracked correctly without having to add the code to each individual class.

**Demo Application** - Check out the demo application in demo\ to see all of the features of the Android wrapper demonstrated.

**NoClassDefFoundError com.google.gson.Gson** - If you are having this error it is because you don't have access to the GSON class files. You may need to copy gson-X.X.X.jar from the libs folder of the wrapper to your application's libs folder.

**Have you called startSession(Context) in onResume()** - If you are getting this warning message even though you already do call startSession() in onResume() then it means you are trying to create events between the last activity's onPause and the new activity's onResume(). E.g. you may be creating events in onCreate() of the new activity which will proceed the onResume() call. You can solve this by simply calling startSession() in onCreate too. It doesn't matter if it's called twice, the sessions will still be logged correctly.

Notable Contributions
=====================

This wrapper uses an altered version of the Android Asynchronous Http Client created by James Smith.
