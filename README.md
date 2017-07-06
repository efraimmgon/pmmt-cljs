# Criminal Analysis & Geoprocessing

In law enforcement understanding how, where, and when crime happens goes a 
long way to helping prevent similar crimes from happening in the future.

Given that old school manual criminal analysis with Excel tables is very 
boring, and the patterns and repetition present in this task, 
criminal analysis is a great candidate for automation.

By adding geoprocessing to the equation we get a very user friendly system,
as we can crunch an arbitrary chunk of data for the user with only a few inputs

With that we can give him a detailed summary with the statistics he needs 
coupled with a visual representation of those statistics, greatly improving 
his comprehension and decision making.

## Features

+ Report generation
	+ crime comparison between two distinct periods
	+ target specific crimes
	+ target specific neighborhoods or venues
	+ breakdown by each day of the week
	+ breakdown by hour of the day
+ Geoprocessing
	+ sync the crime's location with Google Maps' API based, based on its address
	+ visually represent crimes type and location
	+ visual representation as marker or heatmap
	+ filter by: crime, date, or/and time

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run

To start the ClojureScript compiler with live code reloading, run:

	lein figwheel

## License

Free to use and modify.

*Note: `PMMT` stands to `Polícia Militar do Estado de Mato Grosso`*
