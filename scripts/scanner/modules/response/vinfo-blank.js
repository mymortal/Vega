var module = {
  name: "Empty Reponse Body Module",
  type: "response-processor"
};

function run(request, response, ctx) {

  if (response.bodyAsString == "") {

    var i = 0;
    var found = 0;
    var headers = response.allHeaders

    for (i = 0; i < headers.length; i++) {
      if (headers[i].name.toLowerCase() == "location") found = 1;
    }
    if (!found) {
      if ((response.code != 401) && (response.code != 304) && (response.code != 404) && (response.code != 204)) {
        ctx.alert("vinfo-blank", request, response, {
          resource: request.requestLine.uri,
          key: "vinfo-blank" + request.requestLine.uri
        });
      }
    }
  }
}
