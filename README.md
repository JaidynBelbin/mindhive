# mindhive
Application for the MindHive Podcasting Network

The two podcasts (Mindhive and Kinwomen) are now displayed in their own tab, which can be scrolled. An ArrayAdapter which utilises the ViewHolder pattern has been attached to each list. 

Login has also been rejigged; the Google Sign-in is now integrated with Firebase, which allows me to check if the returning user is already logged in, in which case it will send them directly to the next screen. 

The S3 bucket the podcasts are stored in is now configured to send a message to an SQS queue when something is modified (i.e. added, deleted, renamed, etc.). The app checks for these messages on startup, and if found, redownloads the podcasts in the background and updates the Adapter. 
Each time the podcasts are downloaded, they are immediately saved to SharedPreferences, so they are instantly displayed when the user opens the app, and it reduces network usage.

The podcast titles are now formatted to remove the bucket prefix (i.e. mindhive podcast/) and suffix (i.e. .mp3) from each String for readability.

Next update will be sorting them in numerical order. 

Jaidyn
