---
title: unsafeMetadata
---
//[Clerk Android](../../../index.html)/[com.clerk.model.user](../index.html)/[User](index.html)/[unsafeMetadata](unsafe-metadata.html)



# unsafeMetadata



[androidJvm]\




@SerialName(value = &quot;unsafe_metadata&quot;)



val [unsafeMetadata](unsafe-metadata.html): JsonObject? = null



Metadata that can be read and set from the Frontend API. One common use case for this attribute is to implement custom fields that will be attached to the User object. Please note that there is also an unsafeMetadata attribute in the SignUp object. The value of that field will be automatically copied to the user's unsafe metadata once the sign up is complete.




