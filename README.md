Android-Wrapper-Private
=======================

This is the Android wrapper for GameAnalytics. It allows you to get analytics data from your Android game to your GameAnalytics account with the minimum of effort!

**How it works - a brief description!**

Every time a new event is created, the details are stored in a local SQLite database. In addition, a 'BatchThread' is started if one does not already exist. The 'BatchThread' waits for a specific interval and checks whether an internet connection is available. If so, it takes all the event info collected so far, puts them into JSON arrays and sends them off to the Game Analytics server.

If no internet connection is available then the thread will quietly poll the connection until it is resumed. As the events are stored in an SQLite database, your event information is safe even if the user resets their device before the connection is restored.

**Step 1 - Getting the code!**

You can get a copy of the GameAnalytics Android wrapper by either:

- Forking/cloning using Git (all you GitHub afficianados will already know how to do this!)
- Downloading directly as a .zip file using the button near the top of the screen

**Step 2 - Using it in your application**

There are two ways to get the wrapper into your application.

1. Using a library project:
	* Import the entire Game-Analytics-Wrapper project folder into Eclipse using File -> Import -> Existing Android Code Into Workspace.
	* Right click on the newly imported project and click Properties -> Android -> check 'Is Library'
	* Right click on your application that you want to use Sprite Batcher in and click Properties -> Android -> Under libraries click 'add' and select the Android-Sprite-Batcher.

2. Using JARs:
	* Copy the game-analytics-wrapper-vX.X.jar from the releases folder into the lib folder of your Android project.
	* Copy the gson-X.X.X.jar from the libs folder into the lib folder of your Android project
	* *Note: the jar may not be as up-to-date as the code on GitHub. Also you will not have access to the javadoc markup by going this route, whereas if you use the library project you will.*

**Step 3 - Basic setup**

If you haven’t already, sign up for an account at www.gameanalytics.com. Make sure you write down your GAME KEY and SECRET KEY somewhere safe.

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

That’s it! Your basic Game Analytics functionality is set up.

**Step 4 - Logging events**

You can read all about how the event logging system works with Game Analytics [here](http://support.gameanalytics.com/entries/22645043-Event-categories).

To log the different types of events you can use the following methods:

```java

GameAnalytics.newDesignEvent(eventId, value, area, x, y, z);
GameAnalytics.newBusinessEvent(eventId, currency, amount, area, x, y, z);
GameAnalytics.newUserEvent(eventId, gender, birthYear, friendCount, area, x, y, z);
GameAnalytics.newQualityEvent(eventId, message, area, x, y, z);

```

Use colons within the eventId string to denote subtypes. The area, x, y and z parameters are optional. If the area parameter is omitted then it defualts to the class name of the current activity.

**Step 5 - Extra features**

The Android wrapper has a load of extra features to save you the work of coding them yourself. Here's a summary:

**Log Unhandled Exceptions** - Use the following code to automatically log unhandled exceptions:

    GameAnalytics.logUnhandledExceptions();

You can view them by going to the Quality tab of the Game Analytics dashboard and clicking on the EventId called ‘Exceptions’. To view the stack traces, simply click on the individual names of each exception.

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

After an activity calls stopSession() during its oonPause() method, a session time out is started. If another activity calls onResume() and therefore startSession() within this time out interval then the same gaming session is continued instead of starting a new one. By default this time out interval is 10 seconds. You can change this interval using:

    GameAnalytics.setSessionTimeOut(int millis);

*Custom log level* - By default the Game Analytics wrapper will only to the Android operating system log upon a warning or error. However to help during development it is recommended that you set the log level to VERBOSE with the following line of code:

    GameAnalytics.setDebugLogLevel(GameAnalytics.VERBOSE);

This will post logs every time an event is created and batched off to the server. *Place this before you call initialise() to ensure you see all logs.*

**Step 6 - Tips and tricks**

**Parent Activity**
Use a parent class that extends Activity to remove the need to put startSession() and stopSession() in every activity's onPause and onResume() methods. The following example is taken from the demo application:

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

**Demo Application**
Check out the demo application in demo\ to see all of the features of the Android wrapper demonstrated.

**NoClassDefFoundError com.google.gson.Gson**

If you are having this error it is because you don't have access to the GSON class files. You may need to copy gson-X.X.X.jar from the libs folder of the wrapper to your applications libs folder.