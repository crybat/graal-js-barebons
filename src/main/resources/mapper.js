/**
 * Maps an array of Original types to an array of intermediate
 * JSON representation of Result type.
 *
 * @param {Array} objs the array being mapped
 * @return {*} the mapped array
 */
function mapArray(objs) {
  return objs.map((o) => {
    return {
      'map1': o.getEl1(),
      'map2': o.getEl2(),
      'max': Math.max(o.getI(), 0),
    };
  });
}
