# conway-life-stream-server
Conway's Game of Life Streaming Server (Akka implementation)


## Actor hierarchy
* FieldSupervisor (1..1)
    * FieldState (1..1)
        * Cell (1..*)
    