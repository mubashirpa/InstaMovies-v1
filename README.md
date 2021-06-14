## InstaMovies for Android
Insta Movies is an Ancroid app which alows you to watch, stream and download FREE and 1080p HD TV shows and movies on your Android devices. It provides almost latest TV shows and movies. Absolutely free. You can download them on your Android device or watch online. Watching movies and TV shows are the best entertainment!

### Variables
**Webview custom URLs**
| Links | Uses |
| :---------- | :---------- |
| link:// | Open URL in HiddenWebActivity |
| link1:// | Open URL in WebActivity |
| link2:// | Open URL in system default browser |
| link3:// | Open URL in WebActivity or system default browser according to settings |
| video:// | Open URL in YouTubePlayerActivity if URL starts with https://youtu.be/ else open URL in PlayerActivity |
| movie:// | Open URL in MovieDetailsActivity |

**Database keys**
| Keys | Uses |
| :---------- | :---------- |
| Link | Open URL in HiddenWebActivity |
| Link1 | Open URL in WebActivity |
| Link2 | Open URL in system default browser |
| Link3 | Open URL in WebActivity or system default browser according to settings |
| Video | Open URL in YouTubePlayerActivity if URL starts with https://youtu.be/ else open URL in PlayerActivity |
| Movie | Open URL in MovieDetailsActivity |


### To-do
**New/Changes**
- Adult text position in Movie Details Activity
- Don't show background image in banner main if contains video
- Copy magnet link option for hidden web and movie details activities
- Categorize errors in Webviews
- Migrate to Material theme
- Add link:// option also in hidden web activity to open once more
- Change max lines to 4 in movie details activity summary
- Download popup hosts file using download manager and also fetch ad hosts links online

**Bugs/Crashes**
- Request dialog crash on success
