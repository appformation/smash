<img src=assets/logo.png width=500 height=202 />

[![Build Status](https://travis-ci.org/appformation/smash.svg?branch=master)](https://travis-ci.org/appformation/smash)

Smash is Volley inspired networking library that's using [OkHttp][okhttp] in its core.

Usage
-----

Add Smash library to your project `build.gradle`
```groovy
dependencies
{
    compile 'pl.appformation:smash:0.2.0'
}
```

Instantiate one shared SmashQueue object (e.g. in your Application class):
```java
mSmashQueue = Smash.buildSmashQueue(getApplicationContext());
```

To request content of server as String do following:
```java
String url = "https://github.com/appformation/smash";
SmashStringRequest request = new SmashStringRequest(SmashRequest.Method.GET, url,
        new SmashResponse.SuccessListener<String>()
        {
            public void onResponse(String response)
            {
                handleMyNiftyResponse(response);
            }
        }, new SmashResponse.FailedListener()
        {
            public void onFailedResponse(SmashError error)
            {
                showImpendingDoomError(error);
            }
        });
```

Request object looks definitely cooler with [Retrolambda] library:
```java
SmashStringRequest request = new SmashStringRequest(SmashRequest.Method.GET, url,
    this::handleNiftyResponse, this::showImpendingDoomError);
```

Constructed request this way we now need to add to our SmashQueue:
```java
mSmashQueue.add(request);
```

Adding custom headers to request:
```java
SmashStringRequest request = ...
request.setHeaders(Headers.of("CustomHeader", "Value",
                              "AnotherHeader", "AnotherValue"));
```

Why another library?
--------------------

Some of us like Volley library, it's easy to use and has superb extensibility with Request class, but recent removal of HttpClient in Marshmalow made library obsolete. We also don't like reactive programming. With this in mind came idea to rewrite Volley to use directly all of the internals of [OkIo] and [OkHttp] libraries.


What's different than in Volley
-------------------------------

* URL address can be modified, up to the point of reaching dispatcher queue
* [Source] class from OkIo is used instead of InputStream
* Ability to modify User-Agent field globally for all requests
* Both listeners are obligatory in request constructor
* To prevent class names conflicting with OkHttp all smash classes have Smash prefix


Things to do
------------

* Retry policy (for now default from OkHttp is used)
* Authentication mechanism in requests
* [Moshi] support (thanks to OkIo we can use Source class with Moshi)
* Support for multipart/form-data upload
* ... and last but not least, unit tests


Changelog
---------

[Current version is 0.2.0](CHANGELOG.md)


Dependencies of version 0.2.0
-----------------------------

* com.squareup.okhttp:okhttp 2.4.0
* com.squareup.okhttp:okhttp-urlconnection 2.4.0
* com.android.support:support-annotations 23.1.1


Dependencies of development version
-----------------------------------

* com.squareup.okhttp3:okhttp 3.2.0
* com.squareup.okhttp3:okhttp-urlconnection 3.2.0
* com.android.support:support-annotations 23.2.0


License
-------

    Copyright (C) 2015 Appformation Sp. z o.o.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 [okhttp]: http://square.github.io/okhttp/
 [okio]: http://github.com/square/okio/
 [moshi]: http://github.com/square/moshi/
 [source]: https://square.github.io/okio/okio/Source.html
 [retrolambda]: https://github.com/orfjackal/retrolambda
