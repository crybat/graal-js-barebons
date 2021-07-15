const OriginalType = Java.type('pintomau.js.Original');
const ResultType = Java.type('pintomau.js.Result');

/**
 * Maps an array of {@link OriginalType} to an array of {@link ResultType}
 *
 * @param {Array.<OriginalType>} objs the objects to map from
 * @return {Array.<ResultType>} the objects to map to
 */
function mapArray(objs) {
  return objs.map((o) => {
    return new ResultType(
        o.getEl1(), o.getEl2(), Math.max(o.getI(), 0));
  });
}
