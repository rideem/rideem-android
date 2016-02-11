# rideem API - Android client

An android client for interacting with https://rideem.io — a free, simple promo code distribution service

### Installation

The client is a self-contained single file: Rideem.java
Clone this project and copy it along side your app source.

```bash
$ git clone https://github.com/rideem/rideem-android.git
$ cp -r rideem-android/src/io $YOUR_APP_SRC/
```

### Use It

```java
import io.rideem.api.Rideem
Rideem rideem = Rideem.create();
```

### Redeeming Codes

```java
String code = rideem.from("app").get().code;

```

### Requesting Apps

```java
int number_of_requests = rideem.request("app").get();

```

### Example

Check the example/ folder for an example activity.


