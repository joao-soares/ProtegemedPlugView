# ProtegemedPlugView

Software to view used time from Protegemed plugs

##Usage

To use this software, you must have connected on target environment over a VPN connection, or execute in local mode after setting up a config file that points to local database.

On the home screen, you should select the plugs, and fill the interval, then press "Search" button.

##Results

As result, you will have a table with detailed information about the selected plugs.
- Uses: the number of individual plug uses (noted by a 'on' and a 'off' registered at database)
- Simultaneous: the number of 'on' records that occurred simultaneous at the same and others plugs
- Concurrents: the number of events at the one plug that had a time range concurrent at other plugs
- Concurrences: the number of events at the other plugs that one plug is concurrent (equals or greater than previous column)
- Concurrency time: the total concurrency time of the plug
- Used time: the total time of the plug uses
- Average time: result of total time divided by uses
- Smaller use: the smaller duration of a individual use of a plug
- Higher use: the higher duration of a individual use of a plug
- Exceeded time: the number of plug uses that has been discarded by exceeded time defined on properties file
- 'Turns on' and 'Turns off' discarded: the number of records that were discarded because they did not have their counterpart in sequence

##About

This software has been developed as part of my final research to Computer Science graduation, developed under supervision of Md. Marcelo Trindade Rebonatto at Universidade de Passo Fundo
