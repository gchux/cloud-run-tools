import sys
import traceback
from typing import Dict, List, Optional, Union

import requests
import typer
from requests import request
from requests.models import CaseInsensitiveDict
from typing_extensions import Annotated


class Curl:
    def __init__(
        self,
        url: str,
        method: Optional[str],
        headers: Optional[List[str]] = [],
        data_raw: Optional[str] = None,
    ):
        self.url = url
        self.headers = headers
        self.method = method
        self.data_raw = data_raw

    def __method(self) -> str:
        if self.method == None:
            return "GET"
        return self.method

    def __headers(self) -> Dict[str, str]:
        cmdHeaders = self.headers or []
        headers = {}
        for cmdHeader in cmdHeaders:
            parts = cmdHeader.split(":", 2)
            if len(parts) == 2:
                headers[parts[0].strip()] = parts[1].strip()
        return headers

    def __printHeaders(
        self, headers: Union[Dict[str, str], CaseInsensitiveDict[str]] = {}
    ) -> None:
        for name, value in headers.items():
            print(f"\t - Header[{name}]={value}")

    def __printRequest(self, method: str = "GET", headers: Dict[str, str] = {}) -> None:
        print(f"* Request: {method} {self.url}\n\nRequest Headers:")
        self.__printHeaders(headers)
        print("\n* Request Body:\n\t", self.data_raw)

    def __printResponse(self, response: requests.Response):
        print(f"\n* Response: {response.status_code}")
        print("\n* Response Headers:")
        self.__printHeaders(response.headers)
        print("\n* Response Body:")
        print("\t", response.text)

    def __doRequest(
        self, method: str = "GET", headers: Dict[str, str] = {}
    ) -> Optional[requests.Response]:
        try:
            return request(method, self.url, headers=headers, data=self.data_raw)
        except Exception:
            print("\nerror:", traceback.format_exc())
        return None

    def exec(self):
        method = self.__method()
        headers = self.__headers()
        self.__printRequest(method, headers)
        response = self.__doRequest(method, headers)
        if response is None:
            sys.exit(1)
        self.__printResponse(response)


# must match: https://curl.se/docs/manpage.html
def main(
    url: str,
    # https://curl.se/docs/manpage.html#-X
    method: Annotated[Optional[str], typer.Option("--request", "-X")] = "GET",
    # https://curl.se/docs/manpage.html#-H
    header: Annotated[Optional[List[str]], typer.Option("--header", "-H")] = [],
    # https://curl.se/docs/manpage.html#--data-raw
    data_raw: Annotated[Optional[str], typer.Option("--data-raw")] = None,
):
    curl = Curl(url, method, header, data_raw)
    curl.exec()


if __name__ == "__main__":
    typer.run(main)
