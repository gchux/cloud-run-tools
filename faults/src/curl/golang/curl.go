package main

import (
	"bytes"
	"context"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"

	"github.com/urfave/cli/v3"
)

type (
	curl struct{}

	CommandExecutor interface {
		Exec(context.Context, *cli.Command)
	}
)

// must match: https://curl.se/docs/manpage.html
var flags = []cli.Flag{
	// https://curl.se/docs/manpage.html#-X
	&cli.StringFlag{
		Name:     "request",
		Aliases:  []string{"X"},
		Usage:    "HTTP method to be used",
		Required: false,
	},
	// https://curl.se/docs/manpage.html#-H
	&cli.StringSliceFlag{
		Name:     "header",
		Aliases:  []string{"H"},
		Usage:    "HTTP metadata; headers to be sent",
		Required: false,
	},
	// https://curl.se/docs/manpage.html#--data-raw
	&cli.StringFlag{
		Name:     "data-raw",
		Usage:    "data to be send as HTTP payload",
		Required: false,
	},
}

func (c *curl) addHeader(
	request *http.Request,
	name, value *string,
) {
	headerName := strings.TrimSpace(*name)
	headerValue := strings.TrimSpace(*value)

	if headerName == "" {
		return
	}

	if strings.EqualFold(headerName, "Host") {
		request.Host = headerValue
	} else {
		request.Header.Add(headerName, headerValue)
	}
}

func (c *curl) setHeaders(cmd *cli.Command, request *http.Request) {
	for _, cmdHeader := range cmd.StringSlice("header") {
		headerParts := strings.SplitN(cmdHeader, ":", 2)
		if len(headerParts) == 2 {
			c.addHeader(request, &headerParts[0], &headerParts[1])
		}
	}
}

func (c *curl) method(cmd *cli.Command) string {
	if method := cmd.String("request"); method == "" {
		return "GET"
	} else {
		return method
	}
}

func (c *curl) url(
	cmd *cli.Command,
) (string, error) {
	if url := cmd.Args().Get(0); url == "" {
		return "", fmt.Errorf("missing URL")
	} else {
		return url, nil
	}
}

func (c *curl) data(
	cmd *cli.Command,
) *bytes.Buffer {
	if data := cmd.String("data-raw"); data == "" {
		return bytes.NewBuffer([]byte{})
	} else {
		cleanData := strings.TrimSpace(data)
		return bytes.NewBuffer([]byte(cleanData))
	}
}

func (c *curl) printHeaders(headers map[string][]string) {
	for name, value := range headers {
		fmt.Fprintf(os.Stdout, "\t- Header[%s]=%v\n", name, value)
	}
}

func (c *curl) printBody(body io.ReadCloser) {
	if body != nil {
		io.Copy(os.Stdout, body)
	}
	fmt.Fprintln(os.Stdout, "")
}

func (c *curl) printRequest(url *string, request *http.Request) {
	fmt.Fprintf(os.Stdout, "* Request: %s %s\n", request.Method, *url)
	fmt.Fprint(os.Stdout, "\n* Request Hedaers:\n")
	c.printHeaders(request.Header)
	fmt.Fprint(os.Stdout, "\n* Request Body:\n\t")
	c.printBody(request.Body)
}

func (c *curl) printResponse(response *http.Response) {
	fmt.Fprintf(os.Stdout, "* Response: %d\n", response.StatusCode)
	fmt.Fprint(os.Stdout, "\n* Response Hedaers:\n")
	c.printHeaders(response.Header)
	fmt.Fprint(os.Stdout, "\n* Response Body:\n\t")
	c.printBody(response.Body)
}

func (c *curl) Exec(ctx context.Context, cmd *cli.Command) error {
	url, err := c.url(cmd)
	if err != nil {
		return err
	}

	method := c.method(cmd)

	var request *http.Request
	if data := c.data(cmd); data.Len() == 0 {
		request, err = http.NewRequestWithContext(ctx, method, url, nil)
	} else {
		request, err = http.NewRequestWithContext(ctx, method, url, data)
	}

	if err != nil {
		return err
	}

	c.setHeaders(cmd, request)

	c.printRequest(&url, request)

	client := &http.Client{}
	if response, err := client.Do(request); err != nil {
		return err
	} else {
		c.printResponse(response)
	}

	return nil
}

func newCurlCommand() *cli.Command {
	curl := &curl{}

	return (&cli.Command{
		Flags:  flags,
		Action: curl.Exec,
	})
}

func main() {
	cmd := newCurlCommand()

	if err := cmd.Run(context.Background(), os.Args); err != nil {
		fmt.Fprintf(os.Stderr, "\nerror: %v\n", err)
		os.Exit(1)
	}
}
