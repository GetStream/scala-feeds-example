import java.io.File
import java.net.URL
import java.time.LocalDateTime
import java.util.Date

import io.getstream.client.Client
import io.getstream.core.models._
import io.getstream.core.options._
import io.getstream.core.utils.Enrichment.{createCollectionReference, createUserReference}
import io.getstream.core.{KeepHistory, LookupKind, Region}
import retrocompat.FutureConverters._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters._

object Main {
  private val apiKey = "gp6e8sxxzud6"
  private val secret = "7j7exnksc4nxy399fdxvjqyqsqdahax3nfgtp27pumpc7sfm9um688pzpxjpjbf2"

  def main(args: Array[String]): Unit = {
    val client = Client.builder(apiKey, secret).build

    val chris = client.flatFeed("user", "chris")
    val jack = client.flatFeed("timeline", "jack")
    val activity = Activity.builder
      .actor("chris")
      .verb("add")
      .`object`("picture:404")
      // message is a custom field - tip: you can add unlimited custom fields!
      .extraField("message", "Beautiful lion!")
      .build

    for {
      _ <- chris.addActivity(activity).asScala
      // Create a following relationship between Jack's "timeline" feed and Chris' "user" feed:
      _ <- jack.follow(chris).asScala
      // Read Jack's timeline and Chris' post appears in the feed:
      activities <- jack.getActivities(new Limit(10)).asScala
      activity <- activities.asScala
    } println(activity.getID)
  }

  def Example(): Unit = {
    var client = Client.builder(apiKey, secret).build

    // Instantiate a feed object
    val userFeed = client.flatFeed("user", "1")

    // Add an activity to the feed, where actor, object and target are references to objects (`Eric`, `Hawaii`, `Places to Visit`)
    var activity = Activity.builder
      .actor("User:1")
      .verb("pin")
      .`object`("Place:42")
      .target("Board:1")
      .build
    userFeed.addActivity(activity).join

    // Create a bit more complex activity
    activity = Activity.builder
      .actor("User:1")
      .verb("run")
      .`object`("Exercise:42")
      .foreignID("run:1")
      .extra(Map[String, AnyRef](
        "course" -> Map(
          "name" -> "Golden Gate park",
          "distance" -> 10
        ),
        "participants" -> List("Thierry", "Tommaso"),
        "started_at" -> LocalDateTime.now,
        "location" -> Map(
          "type" -> "point",
          "coordinates" -> List(37.769722, -122.476944)
        )
      ).asJava).build
    userFeed.addActivity(activity).join

    // Remove an activity by its id
    userFeed.removeActivityByID("e561de8f-00f1-11e4-b400-0cc47a024be0").join

    // Remove activities with foreign_id 'run:1'
    userFeed.removeActivityByForeignID("run:1").join

    activity = Activity.builder
      .actor("1")
      .verb("like")
      .`object`("3")
      .time(new Date)
      .foreignID("like:3")
      .extraField("popularity", 100)
      .build

    // first time the activity is added
    userFeed.addActivity(activity).join

    // update the popularity value for the activity
    activity = Activity.builder.fromActivity(activity).extraField("popularity", 10).build

    client.batch.updateActivities(activity).join

    // partial update by activity ID

    // prepare the set operations
    val set = Map[String, AnyRef](
      "product.price" -> 19.99: (String, Number),
      "shares" -> Map(
        "facebook" -> "...",
        "twitter" -> "..."
      )
    )
    // prepare the unset operations
    val unset = List("daily_likes", "popularity")

    val id = "54a60c1e-4ee3-494b-a1e3-50c06acb5ed4"
    client.updateActivityByID(id, set.asJava, unset.asJava).join

    val foreignID = "product:123"
    val timestamp = new Date
    client.updateActivityByForeignID(foreignID, timestamp, set.asJava, unset.asJava)

    val add = List[FeedID]()
    val remove = List[FeedID]()
    userFeed.updateActivityToTargets(activity, add.asJava, remove.asJava)

    val newTargets = List[FeedID]()
    userFeed.replaceActivityToTargets(activity, newTargets.asJava)


    val now = new Date
    val firstActivity = userFeed.addActivity(Activity.builder
      .actor("1")
      .verb("like")
      .`object`("3")
      .time(now)
      .foreignID("like:3")
      .build).join
    val secondActivity = userFeed.addActivity(Activity.builder
      .actor("1")
      .verb("like")
      .`object`("3")
      .time(now)
      .extraField("extra", "extra_value")
      .foreignID("like:3")
      .build).join
    // foreign ID and time are the same for both activities
    // hence only one activity is created and first and second IDs are equal
    // firstActivity.ID == secondActivity.ID


    // Get 5 activities with id less than the given UUID (Faster - Recommended!)
    var response = userFeed.getActivities(new Limit(5), new Filter().idLessThan("e561de8f-00f1-11e4-b400-0cc47a024be0")).join
    // Get activities from 5 to 10 (Pagination-based - Slower)
    response = userFeed.getActivities(new Limit(5), new Offset(0)).join
    // Get activities sorted by rank (Ranked Feeds Enabled):
    response = userFeed.getActivities(new Limit(5), "popularity").join


    // timeline:timeline_feed_1 follows user:user_42
    val user = client.flatFeed("user", "user_42")
    val timeline = client.flatFeed("timeline", "timeline_feed_1")
    timeline.follow(user).join

    // follow feed without copying the activities:
    timeline.follow(user, 0).join



    // user := client.FlatFeed("user", "42")

    // Stop following feed user:user_42
    timeline.unfollow(user).join

    // Stop following feed user:user_42 but keep history of activities
    timeline.unfollow(user, KeepHistory.YES).join

    // list followers
    val followers = userFeed.getFollowers(new Limit(10), new Offset(0)).join
    for (follow <- followers.asScala) {
      System.out.format("%s -> %s", follow.getSource, follow.getTarget)
    }

    // Retrieve last 10 feeds followed by user_feed_1
    var followed = userFeed.getFollowed(new Limit(10), new Offset(0)).join

    // Retrieve 10 feeds followed by user_feed_1 starting from the 11th
    followed = userFeed.getFollowed(new Limit(10), new Offset(10)).join

    // Check if user_feed_1 follows specific feeds
    followed = userFeed.getFollowed(new Limit(2), new Offset(0), new FeedID("user:42"), new FeedID("user", "43")).join


    val notifications = client.notificationFeed("notifications", "1")
    // Mark all activities in the feed as seen
    var activityGroups = notifications.getActivities(new ActivityMarker().allSeen).join
    for (group <- activityGroups.asScala) {
    }
    // Mark some activities as read via specific Activity Group Ids
    activityGroups = notifications.getActivities(new ActivityMarker().read("groupID1", "groupID2" /* ... */)).join



    // Add an activity to the feed, where actor, object and target are references to objects - adding your ranking method as a parameter (in this case, "popularity"):
    activity = Activity.builder
      .actor("User:1")
      .verb("pin")
      .`object`("place:42")
      .target("board:1")
      .extraField("popularity", 5)
      .build
    userFeed.addActivity(activity).join

    // Get activities sorted by the ranking method labelled 'activity_popularity' (Ranked Feeds Enabled)
    response = userFeed.getActivities(new Limit(5), "activity_popularity").join



    // Add the activity to Eric's feed and to Jessica's notification feed
    activity = Activity.builder
      .actor("User:Eric")
      .verb("tweet")
      .`object`("tweet:id")
      .to(List(new FeedID("notification:Jessica")).asJava)
      .extraField("message", "@Jessica check out getstream.io it's so dang awesome.")
      .build
    userFeed.addActivity(activity).join

    // The TO field ensures the activity is send to the player, match and team feed
    activity = Activity.builder
      .actor("Player:Suarez")
      .verb("foul")
      .`object`("Player:Ramos")
      .to(List(new FeedID("team:barcelona"), new FeedID("match:1")).asJava)
      .extraField("match", Map("El Classico" -> 10)).build
    // playerFeed.addActivity(activity);
    userFeed.addActivity(activity)


    // Batch following many feeds
    // Let timeline:1 will follow user:1, user:2 and user:3
    val follows = List(new FollowRelation("timeline:1", "user:1"), new FollowRelation("timeline:3", "user:2"), new FollowRelation("timeline:1", "user:3"))
    client.batch.followMany(follows.asJava).join
    // copy only the last 10 activities from every feed
    client.batch.followMany(10, follows.asJava).join


    val activities = List(
      Activity.builder
        .actor("User:1")
        .verb("tweet")
        .`object`("Tweet:1")
        .build,
      Activity.builder
        .actor("User:2")
        .verb("watch")
        .`object`("Movie:1")
        .build
    )
    userFeed.addActivities(activities.asJava)



    // adds 1 activity to many feeds in one request
    activity = Activity.builder
      .actor("User:2")
      .verb("pin")
      .`object`("Place:42")
      .target("Board:1")
      .build
    val feeds = Array(new FeedID("timeline", "1"), new FeedID("timeline", "2"), new FeedID("timeline", "3"), new FeedID("timeline", "4"))
    client.batch.addToMany(activity, feeds: _*).join



    // retrieve two activities by ID
    client.batch.getActivitiesByID("01b3c1dd-e7ab-4649-b5b3-b4371d8f7045", "ed2837a6-0a3b-4679-adc1-778a1704852").join

    // retrieve an activity by foreign ID and time
    client.batch.getActivitiesByForeignID(new ForeignIDTimePair("foreignID1", new Date), new ForeignIDTimePair("foreignID2", new Date))



    // connect to the us-east region
    client = Client.builder(apiKey, secret).region(Region.US_EAST).build


    var like = Reaction.builder
      .kind("like")
      .activityID(activity.getID)
      .build

    // add a like reaction to the activity with id activityId
    like = client.reactions.add("john-doe", like).join

    var comment = Reaction.builder
      .kind("comment")
      .activityID(activity.getID)
      .extraField("text", "awesome post!")
      .build

    // adds a comment reaction to the activity with id activityId
    comment = client.reactions.add("john-doe", comment).join



    // first let's read current user's timeline feed and pick one activity
    response = client.flatFeed("timeline", "mike").getActivities.join
    activity = response.get(0)

    // then let's add a like reaction to that activity
    client.reactions.add("john-doe", Reaction.builder
      .kind("like")
      .activityID(activity.getID)
      .build)


    comment = Reaction.builder
      .kind("comment")
      .activityID(activity.getID)
      .extraField("text", "awesome post!")
      .build

    // adds a comment reaction to the activity and notify Thierry's notification feed
    client.reactions.add("john-doe", comment, new FeedID("notification:thierry"))



    // read bob's timeline and include most recent reactions to all activities and their total count
    client.flatFeed("timeline", "bob")
      .getEnrichedActivities(new EnrichmentFlags().withRecentReactions.withReactionCounts)
      .join

    // read bob's timeline and include most recent reactions to all activities and her own reactions
    client.flatFeed("timeline", "bob")
      .getEnrichedActivities(new EnrichmentFlags().withOwnReactions.withRecentReactions.withReactionCounts)
      .join


    // retrieve all kind of reactions for an activity
    var reactions = client.reactions.filter(LookupKind.ACTIVITY, "ed2837a6-0a3b-4679-adc1-778a1704852d").join

    // retrieve first 10 likes for an activity
    reactions = client.reactions.filter(LookupKind.ACTIVITY, "ed2837a6-0a3b-4679-adc1-778a1704852d", new Limit(10), "like").join

    // retrieve the next 10 likes using the id_lt param
    reactions = client.reactions.filter(LookupKind.ACTIVITY, "ed2837a6-0a3b-4679-adc1-778a1704852d", new Filter().idLessThan("e561de8f-00f1-11e4-b400-0cc47a024be0"), new Limit(10), "like").join


    // adds a like to the previously created comment
    val reaction = client.reactions.addChild("john-doe", comment.getId, Reaction.builder.kind("like").build).join


    client.reactions.update(Reaction.builder.id(reaction.getId).extraField("text", "love it!").build)


    client.reactions.delete(reaction.getId)


    client.collections.add("food",
      new CollectionData("cheese-burger")
        .set("name", "Cheese Burger")
        .set("rating", "4 stars"))

    // if you don't have an id on your side, just use null as the ID and Stream will generate a unique ID
    client.collections.add("food",
      new CollectionData()
        .set("name", "Cheese Burger")
        .set("rating", "4 stars"))


    val collection = client.collections.get("food", "cheese-burger").join


    client.collections.delete("food", "cheese-burger")


    client.collections.update("food",
      new CollectionData("cheese-burger")
        .set("name", "Cheese Burger")
        .set("rating", "1 star"))


    client.collections.upsert("visitor",
      new CollectionData("123")
        .set("name", "John")
        .set("favorite_color", "blue"),
      new CollectionData("124")
        .set("name", "Jane")
        .set("favorite_color", "purple")
        .set("interests", List("fashion", "jazz")))


    // select the entries with ID 123 and 124 from items collection
    val objects = client.collections.select("items", "123", "124").join



    // delete the entries with ID 123 and 124 from visitor collection
    client.collections.deleteMany("visitor", "123", "124").join


    // first we add our object to the food collection
    val cheeseBurger = client.collections.add("food",
      new CollectionData("123")
        .set("name", "Cheese Burger")
        .set("ingredients", List("cheese", "burger", "bread", "lettuce", "tomato"))).join

    // the object returned by .add can be embedded directly inside of an activity
    userFeed.addActivity(Activity.builder
      .actor(createUserReference("john-doe"))
      .verb("grill")
      .`object`(createCollectionReference(cheeseBurger.getCollection, cheeseBurger.getID))
      .build)

    // if we now read the feed, the activity we just added will include the entire full object
    userFeed.getEnrichedActivities.join

    // we can then update the object and Stream will propagate the change to all activities
    client.collections.update(cheeseBurger.getCollection, cheeseBurger.set("name", "Amazing Cheese Burger").set("ingredients", List("cheese", "burger", "bread", "lettuce", "tomato"))).join



    // First create a collection entry with upsert api
    client.collections.upsert("food", new CollectionData().set("name", "Cheese Burger"))

    // Then create a user
    client.user("john-doe").create(new Data()
      .set("name", "John Doe")
      .set("occupation", "Software Engineer")
      .set("gender", "male"))

    // Since we know their IDs we can create references to both without reading from APIs
    val cheeseBurgerRef = createCollectionReference("food", "cheese-burger")
    val johnDoeRef = createUserReference("john-doe")

    client.flatFeed("user", "john")
      .addActivity(Activity.builder
        .actor(johnDoeRef)
        .verb("eat")
        .`object`(cheeseBurgerRef)
        .build)



    // create a new user, if the user already exist an error is returned
    client.user("john-doe")
      .create(new Data()
        .set("name", "John Doe")
        .set("occupation", "Software Engineer")
        .set("gender", "male"))

    // get or create a new user, if the user already exist the user is returned
    client.user("john-doe")
      .getOrCreate(new Data()
        .set("name", "John Doe")
        .set("occupation", "Software Engineer")
        .set("gender", "male"))


    client.user("123").get


    client.user("123").delete


    client.user("123")
      .update(new Data()
        .set("name", "Jane Doe")
        .set("occupation", "Software Engineer")
        .set("gender", "female"))



    // Read the personalization feed for a given user
    client.personalization.get("123", "personalized_feed", Map[String, AnyRef](
      "user_id" -> 123: (String, Number),
      "feed_slug" -> "timeline"
    ).asJava)

    // Our data science team will typically tell you which endpoint to use
    client.personalization.get("discovery_feed", Map[String, AnyRef](
      "user_id" -> 123: (String, Number),
      "source_feed_slug" -> "timeline",
      "target_feed_slug" -> "user"
    ).asJava)


    client.analytics.trackEngagement(Engagement.builder
      .feedID("user:thierry")
      .content(new Content("message:34349698")
        .set("verb", "share")
        .set("actor", Map("1" -> "user1")))
      .boost(2)
      .location("profile_page")
      .position(3)
      .build)


    client.analytics.trackImpression(Impression.builder
      .contentList(
        new Content("tweet:34349698")
          .set("verb", "share")
          .set("actor", Map("1" -> "user1")),
        new Content("tweet:34349699"),
        new Content("tweet:34349700"))
      .feedID("flat:tommaso")
      .location("android-app")
      .build)


    // the URL to direct to
    val targetURL = new URL("http://mysite.com/detail")

    // track the impressions and a click
    val impressions = List(Impression.builder
      .contentList(new Content("tweet:1"), new Content("tweet:2"), new Content("tweet:3"))
      .userData(new UserData("tommaso", null))
      .location("email")
      .feedID("user:global")
      .build)
    val engagements = List(Engagement.builder
      .content(new Content("tweet:2"))
      .label("click")
      .position(1)
      .userData(new UserData("tommaso", null))
      .location("email")
      .feedID("user:global")
      .build)

    // when the user opens the tracking URL in their browser gets redirected to the target URL
    // the events are added to our analytics platform
    val trackingURL = client.analytics.createRedirectURL(targetURL, impressions.asJava, engagements.asJava)


    val image = new File("...")
    val imageURL = client.images.upload(image).join

    val file = new File("...")
    val fileURL = client.files.upload(file).join



    // deleting an image using the url returned by the APIs
    client.images.delete(imageURL)

    // deleting a file using the url returned by the APIs
    client.files.delete(fileURL)



    // create a 50x50 thumbnail and crop from center
    client.images.process(imageURL, new Resize(50, 50, Resize.Type.CROP))

    // create a 50x50 thumbnail using clipping (keeps aspect ratio)
    client.images.process(imageURL, new Resize(50, 50, Resize.Type.CLIP))


    val urlPreview = client.openGraph(new URL("http://www.imdb.com/title/tt0117500/")).join
  }
}
