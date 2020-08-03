![ci-cd](https://github.com/fedor-malyshkin/conway-life-stream-server/workflows/ci-cd/badge.svg)

# conway-life-stream-server
Conway's Game of Life Streaming Server (Akka implementation)


## Actors' hierarchy
* FieldSupervisor (1)
    * FieldState (1)
    * Field (1) 
        * Cell (1..*)
    