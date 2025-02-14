const { Command } = require('commander');
const axios = require('axios');
const program = new Command();

class Curl {

  constructor(curl) {
    this.curl = curl;
  }

  #headers() {
    var headers = new Map();
    this.curl.headers.forEach(header => {
      const parts = header.split(":", 2);
      if (parts.length == 2) {
        headers.set(parts[0].trim(), parts[1].trim());
      }
    });
    return headers;
  }

  #method(headers) {
    return (this.curl.method || "GET").toLowerCase()
  }

  #logHeaders(isRequest, headers) {
    console.log(`\n* ${isRequest ? "Request" : "Response"} Headers:`)
    for (let [name, value] of headers) {
      console.log(`\t- Header[${name}]=${value}`)
    }
  }

  #logRequest(method, headers) {
    console.log(`* Request: ${method.toUpperCase()} ${this.curl.url}`);
    this.#logHeaders(true, headers)
    console.log("\n * Request Body: ");
    console.log("\t", this.curl.data);
  }

  #logResponse(response) {
    console.log(`\n* Response: ${response.status}`);
    this.#logHeaders(false, response.headers)
    console.log("\n* Response Body: \n\t", response.data)
  }

  #handleResponse(response) {
    const that = this;
    response
      .then((response) => {
        that.#logResponse(response);
      })
      .catch((error) => {
        console.error(error);
      });
  }

  async exec() {
    const method = this.#method();
    const headers = this.#headers();

    this.#logRequest(method, headers)

    try {
      const response = await axios({
        method: method,
        url: this.curl.url,
        headers: headers,
        data: this.curl.data,
      });
      this.#logResponse(response);
    } catch (ex) {
      console.error(ex);
    }
  }

}

const newCurl = function (program) {
  const args = program.args;
  const opts = program.opts()

  console.log(opts);

  const curl = {
    url: args[0],
    method: opts['request'] || "GET",
    headers: opts['header'] || [],
    data: opts['dataRaw'] || "",
  };

  return new Curl(curl);
}

// must match: https://curl.se/docs/manpage.html
program
  // https://curl.se/docs/manpage.html#-X
  .option('-X, --request <string>', 'HTTP method to be used', "")
  // https://curl.se/docs/manpage.html#-H
  .option('-H, --header <value...>', 'HTTP request metadata; headets to be sent', [])
  // https://curl.se/docs/manpage.html#--data-raw
  .option('-D --data-raw <string>', 'data to be sent in the HTTP request payload', "")
  .argument('<string>', 'URL');

program.parse(process.argv);

const curl = newCurl(program);

curl.exec();
